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
    @Autowired private UserRepository userRepository;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;

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
    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    @Transactional
    /**
     * Esta anotacion le dice a Spring que este metodo debe ejecutarse dentro de una transaccion.
     * Lo que ocurra dentro de este metodo debe tratarse como una sola unidad de trabajo de base de datos.
     * Basicamente garantiza atomicidad, es decir que: o se completa toda la compra, o no se hace nada.
     */
    public OrderResponse checkout(Long userId, CheckoutRequest request) {

        Cart cart = lockedCart(userId);
        if (request.getRequestKey() == null || request.getExpectedTotal() == null) {
            throw new InvalidCartException("Se necesita una cotización aceptada y una clave de confirmación");
        }
        var previous = orderRepository.findByUserIdAndRequestKey(userId, request.getRequestKey());
        if (previous.isPresent()) {
            Order saved = previous.get();
            if (saved.getPaymentMethod() != request.getPaymentMethod()
                    || saved.getTotal().compareTo(request.getExpectedTotal()) != 0) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                        "La clave de confirmación ya fue usada con otros datos");
            }
            return getOrderById(userId, saved.getId());
        }
        List<CartItem> items = validatedItems(userId, cart);
        CheckoutPreviewResponse quote = calculate(items, request);
        BigDecimal subtotal = quote.getSubtotal();
        BigDecimal discountTotal = quote.getTotalDiscount();
        BigDecimal total = quote.getTotal();
        if (total.compareTo(request.getExpectedTotal()) != 0) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "El total cambió. Revisá la cotización actualizada antes de confirmar nuevamente.");
        }

        ///Ahora se crea la orden de compra persistente, queda pendiente porque el checkout no procesa el pago.
        Order order = new Order();

        order.setUser(cart.getUser());
        order.setRequestKey(request.getRequestKey());
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

            Product product = item.getProduct();

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


            ///Descuenta el stock
            int newStock =
                    product.getStock() - item.getQuantity();

            product.setStock(newStock);

            ///Si el stock es cero, se marca el producto como agotado
            if (newStock == 0) {
                product.setStatus(ProductStatus.AGOTADO);
            }

            productRepository.save(product);
        }

        ///Convertimos a orderPromotion y guardamos en la base de datos, para cada promocion aplicada.
        List<OrderPromotion> savedOrderPromotions =
                new ArrayList<>();

        for (AppliedPromotionResponse appliedPromotion
                : quote.getPromotions()) {

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


    // Locks in ascending product ID order prevent overselling and crossed-cart deadlocks.
    // Refresh matters: CartItem may already have loaded Product in this persistence context.
    private List<CartItem> validatedItems(Long userId, Cart cart) {
        List<CartItem> items = cartItemRepository.findByCart_Id(cart.getId());
        if (items.isEmpty()) throw new InvalidCartException("No se puede comprar con el carrito vacío");
        items.sort(java.util.Comparator.comparing(item -> item.getProduct().getId()));
        for (CartItem item : items) {
            entityManager.refresh(item.getProduct(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        }

        for (CartItem item : items) {

            Product product = item.getProduct();

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

        }
        return items;
    }

    private CheckoutPreviewResponse calculate(List<CartItem> items, CheckoutRequest request) {
        PromotionApplicationResponse result = promotionService.applyPromotions(items, request.getPaymentMethod());
        CheckoutPreviewResponse preview = new CheckoutPreviewResponse();
        BigDecimal subtotal = items.stream().map(item -> item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        preview.setSubtotal(subtotal);
        preview.setTotalDiscount(result.getTotalDiscount());
        preview.setTotal(subtotal.subtract(result.getTotalDiscount()));
        preview.setPromotions(result.getAppliedPromotions());
        return preview;
    }

    private Cart lockedCart(Long userId) {
        // Same lock used by cart mutations, including creation of the first cart.
        userRepository.lockById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return cartRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));
    }

    @Override
    @Transactional
    public CheckoutPreviewResponse preview(Long userId, CheckoutRequest request) {
        return calculate(validatedItems(userId, lockedCart(userId)), request);
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
