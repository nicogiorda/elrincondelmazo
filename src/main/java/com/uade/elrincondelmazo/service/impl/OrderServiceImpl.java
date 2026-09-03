package com.uade.elrincondelmazo.service.impl;

import com.uade.elrincondelmazo.entity.*;
import com.uade.elrincondelmazo.entity.dto.*;
import com.uade.elrincondelmazo.enums.ProductStatus;
import com.uade.elrincondelmazo.enums.StateOrder;
import com.uade.elrincondelmazo.exception.InvalidCartException;
import com.uade.elrincondelmazo.exception.ResourceNotFoundException;
import com.uade.elrincondelmazo.repository.*;
import com.uade.elrincondelmazo.service.PromotionService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uade.elrincondelmazo.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPromotionRepository orderPromotionRepository;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;

    private final PromotionService promotionService;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            OrderPromotionRepository orderPromotionRepository,
                            CartRepository cartRepository,
                            CartItemRepository cartItemRepository,
                            ProductRepository productRepository,
                            PromotionRepository promotionRepository,
                            PromotionService promotionService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderPromotionRepository = orderPromotionRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.promotionRepository = promotionRepository;
        this.promotionService = promotionService;

    }

    ///Decision importante: Si un usuario no tiene pedidos, se devuelve una lista vacia y no una excepcion
    ///Esto es para evitar manejar "El usuario no registra pedidos" como un error.
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    @Transactional
    /**Esta anotacion le dice a Spring que este metodo debe ejecutarse dentro de una transaccion.
     * Lo que ocurra dentro de este metodo debe tratarse como una sola unidad de trabajo de base de datos.
     * Basicamente garantiza atomicidad, es decir que: o se completa toda la compra, o no se hace nada.
     */
    public OrderResponse checkout(Long userId, CheckoutRequest request) {

        ///Primero se vuelve a validar el carrito y los items, para asegurarse de que no haya cambios desde la ultima vez que se consulto.

        ///Se busca el carrito del ususario
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Carrito no encontrado para el usuario con id: " + userId));


        ///Obtenemos los items del carrito.
        List<CartItem> items =
                cartItemRepository.findByCart_Id(cart.getId());

        if (items.isEmpty()) {
            throw new InvalidCartException(
                    "No se puede realizar checkout con el carrito vacío");
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : items) {

            Product product = productRepository
                    .findById(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con id: "
                                    + item.getProduct().getId()));

            if (product.getStatus() != ProductStatus.ACTIVO) {
                throw new InvalidCartException(
                        "El producto " + product.getId()
                                + " ya no está disponible");
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new InvalidCartException(
                        "Cantidad inválida para el producto "
                                + product.getId());
            }

            if (item.getQuantity() > product.getStock()) {
                throw new InvalidCartException(
                        "Stock insuficiente para el producto "
                                + product.getId());
            }

            if (product.getSeller().getId().equals(userId)) {
                throw new InvalidCartException(
                        "No se puede comprar un producto propio");
            }

            ///Luego para cada Item del carrito se calcula el subtotal obteniendo el precio actual del producto
            ///y multiplicandolo por la cantidad de items dentro del carrito.
            BigDecimal itemSubtotal =
                    product.getPrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            item.getQuantity()));

            subtotal = subtotal.add(itemSubtotal);
        }

        ///Con el subtotal ya calculado, se delega al promotionService las promociones si aplican
        PromotionApplicationResponse promotionResult =
                promotionService.applyPromotions(
                        items,
                        request.getPaymentMethod()
                );


        BigDecimal discountTotal =
                promotionResult.getTotalDiscount();

        BigDecimal total =
                subtotal.subtract(discountTotal);

        ///Ahora se crea la orden de compra persistente, queda pendiente porque el checkout no procesa el pago.
        Order order = new Order();

        order.setUser(cart.getUser());
        order.setOrderDate(LocalDateTime.now());
        order.setState(StateOrder.PENDIENTE);
        order.setPaymentMethod(request.getPaymentMethod());

        order.setSubtotal(subtotal);
        order.setTotalDiscount(discountTotal);
        order.setTotal(total);

        ///Se guarda la orden en la base de datos, esto genera un id para la orden que se usara para los items y promociones.
        order = orderRepository.save(order);



        /// Se guardan los items de la orden
        List<OrderItem> savedOrderItems = new ArrayList<>();

        for (CartItem item : items) {

            Product product = productRepository.findById(
                            item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con id: "
                                    + item.getProduct().getId()));

            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setSeller(product.getSeller());
            orderItem.setQuantity(item.getQuantity());

            /// Precio histórico de la compra
            orderItem.setPrice(product.getPrice());

            savedOrderItems.add(
                    orderItemRepository.save(orderItem)
            );

            int newStock =
                    product.getStock() - item.getQuantity();

            product.setStock(newStock);

            if (newStock == 0) {
                product.setStatus(ProductStatus.AGOTADO);
            }

            productRepository.save(product);
        }

        ///Convertimos a orderPromotion y guardamos en la base de datos, para cada promocion aplicada.
        List<OrderPromotion> savedOrderPromotions =
                new ArrayList<>();

        for (AppliedPromotionResponse appliedPromotion
                : promotionResult.getAppliedPromotions()) {

            Promotion promotion = promotionRepository
                    .findById(appliedPromotion.getPromotionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Promoción no encontrada con id: "
                                    + appliedPromotion.getPromotionId()));

            OrderPromotion orderPromotion =
                    new OrderPromotion();

            orderPromotion.setOrder(order);
            orderPromotion.setPromotion(promotion);
            orderPromotion.setDiscountApplied(
                    appliedPromotion.getDiscountApplied());

            savedOrderPromotions.add(
                    orderPromotionRepository.save(orderPromotion)
            );
        }

        ///Si todo salio bien, se limpian los items del carrito.
        cartItemRepository.deleteAll(items);


        ///Finalmente se mapea la orden, los items y las promociones aplicadas a un OrderResponse y se devuelve al cliente.
        return mapToOrderDetail(
                order,
                savedOrderItems,
                savedOrderPromotions
        );
    }


    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(
            Long userId,
            Long orderId) {

        Order order = orderRepository
                .findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Orden no encontrada con id: " + orderId));

        List<OrderItem> items =
                orderItemRepository.findByOrderId(orderId);

        List<OrderPromotion> promotions =
                orderPromotionRepository.findByOrderId(orderId);

        return mapToOrderDetail(
                order,
                items,
                promotions
        );
    }


    ///Metodo privado para mapear los atributos necesarios de una entidad Order a una OrderResponse
    private OrderResponse mapToResponse(Order order) {

        OrderResponse response = new OrderResponse();

        response.setId(order.getId());
        response.setOrderDate(order.getOrderDate());
        response.setState(order.getState());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setSubtotal(order.getSubtotal());
        response.setTotalDiscount(order.getTotalDiscount());
        response.setTotal(order.getTotal());

        return response;
    }



    ///Metodo privado para mapear los atributos necesarios de una entidad OrderItem a una OrderItemResponse
    private OrderItemResponse mapToOrderItemResponse(
            OrderItem item) {

        OrderItemResponse response =
                new OrderItemResponse();

        response.setId(item.getId());

        response.setProductId(
                item.getProduct().getId());

        response.setProductName(
                item.getProduct().getName());

        response.setSellerId(
                item.getSeller().getId());

        response.setSellerEmail(
                item.getSeller().getEmail());

        response.setQuantity(
                item.getQuantity());

        response.setPrice(
                item.getPrice());

        return response;
    }


    ///Metodo privado para mapear los atributos necesarios de una entidad OrderPromotion a una OrderPromotionResponse
    private OrderPromotionResponse mapToOrderPromotionResponse(
            OrderPromotion orderPromotion) {

        OrderPromotionResponse response =
                new OrderPromotionResponse();

        response.setPromotionId(
                orderPromotion.getPromotion().getId());

        response.setPromotionName(
                orderPromotion.getPromotion().getName());

        response.setDiscountApplied(
                orderPromotion.getDiscountApplied());

        return response;
    }


    /**
     * Metodo privado para mapear los atributos necesarios de una entidad Order, sus OrderItems y OrderPromotions a un OrderResponse completo.
     * @param order
     * @param items
     * @param promotions
     * @return
     */
    private OrderResponse mapToOrderDetail(
            Order order,
            List<OrderItem> items,
            List<OrderPromotion> promotions) {

        OrderResponse response =
                mapToResponse(order);

        response.setItems(
                items.stream()
                        .map(this::mapToOrderItemResponse)
                        .toList()
        );

        response.setPromotions(
                promotions.stream()
                        .map(this::mapToOrderPromotionResponse)
                        .toList()
        );

        return response;
    }

}
