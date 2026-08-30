package com.uade.elrincondelmazo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uade.elrincondelmazo.entity.Cart;
import com.uade.elrincondelmazo.entity.CartItem;
import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.AddCartItemRequest;
import com.uade.elrincondelmazo.entity.dto.CartItemResponse;
import com.uade.elrincondelmazo.entity.dto.CartResponse;
import com.uade.elrincondelmazo.entity.dto.UpdateCartItemQuantityRequest;
import com.uade.elrincondelmazo.enums.ProductStatus;
import com.uade.elrincondelmazo.exception.InvalidCartException;
import com.uade.elrincondelmazo.exception.ResourceNotFoundException;
import com.uade.elrincondelmazo.repository.CartItemRepository;
import com.uade.elrincondelmazo.repository.CartRepository;
import com.uade.elrincondelmazo.repository.ProductRepository;
import com.uade.elrincondelmazo.repository.UserRepository;
import com.uade.elrincondelmazo.service.CartService;

@Service
public class CartServiceImpl implements CartService {

        private final CartRepository cartRepository;
        private final UserRepository userRepository;
        private final CartItemRepository cartItemRepository;
        private final ProductRepository productRepository;

        @Autowired
        public CartServiceImpl(
                        CartRepository cartRepository,
                        UserRepository userRepository,
                        CartItemRepository cartItemRepository, ProductRepository productRepository) {

                this.cartRepository = cartRepository;
                this.userRepository = userRepository;
                this.cartItemRepository = cartItemRepository;
                this.productRepository = productRepository;
        }

        @Override
        public CartResponse getOrCreateCart(Long userId) {

                return toCartResponse(getOrCreateCartEntity(userId));
        }

        @Override
        public CartResponse addItem(Long userId, AddCartItemRequest request) {

                Product product = productRepository.findById(request.getProductId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Producto no encontrado con id: " + request.getProductId()));

                if (request.getQuantity() == null || request.getQuantity() <= 0) {
                        throw new InvalidCartException("La cantidad debe ser mayor a cero");
                }

                if (product.getStatus() != ProductStatus.ACTIVO) {
                        throw new InvalidCartException("El producto no está disponible");
                }

                if (product.getSeller().getId().equals(userId)) {
                        throw new InvalidCartException(
                                        "No se puede agregar al carrito un producto propio");
                }

                Cart cart = getOrCreateCartEntity(userId);

                CartItem item = cartItemRepository
                                .findByCart_IdAndProduct_Id(cart.getId(), product.getId())
                                .orElse(null);

                if (item == null) {

                        if (request.getQuantity() > product.getStock()) {
                                throw new InvalidCartException("Stock insuficiente");
                        }

                        item = new CartItem();
                        item.setCart(cart);
                        item.setProduct(product);
                        item.setQuantity(request.getQuantity());

                } else {

                        int newQuantity = item.getQuantity() + request.getQuantity();

                        if (newQuantity > product.getStock()) {
                                throw new InvalidCartException("Stock insuficiente");
                        }

                        item.setQuantity(newQuantity);
                }

                cartItemRepository.save(item);

                return toCartResponse(cart);
        }

        private Cart createCart(Long userId) {

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Usuario no encontrado con id: " + userId));

                Cart cart = new Cart();
                cart.setUser(user);
                cart.setCreatedAt(LocalDateTime.now());

                return cartRepository.save(cart);
        }

        private Cart getOrCreateCartEntity(Long userId) {
                return cartRepository.findByUserId(userId)
                                .orElseGet(() -> createCart(userId));
        }

        @Override
        public CartResponse updateItemQuantity(
                        Long userId,
                        Long cartItemId,
                        UpdateCartItemQuantityRequest request) {

                if (request.getQuantity() == null || request.getQuantity() <= 0) {
                        throw new InvalidCartException(
                                        "La cantidad debe ser mayor a cero");
                }

                Cart cart = cartRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Carrito no encontrado para el usuario con id: " + userId));

                CartItem item = cartItemRepository.findById(cartItemId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Item del carrito no encontrado con id: " + cartItemId));

                if (!item.getCart().getId().equals(cart.getId())) {
                        throw new ResourceNotFoundException(
                                        "El item no pertenece al carrito del usuario");
                }

                if (request.getQuantity() > item.getProduct().getStock()) {
                        throw new InvalidCartException(
                                        "Stock insuficiente");
                }

                item.setQuantity(request.getQuantity());

                cartItemRepository.save(item);

                return toCartResponse(cart);
        }

        @Override
        public CartResponse removeItem(Long userId, Long cartItemId) {

                Cart cart = cartRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Carrito no encontrado para el usuario con id: " + userId));

                CartItem item = cartItemRepository.findById(cartItemId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Item del carrito no encontrado con id: " + cartItemId));

                if (!item.getCart().getId().equals(cart.getId())) {
                        throw new ResourceNotFoundException(
                                        "El item no pertenece al carrito del usuario");
                }

                cartItemRepository.delete(item);

                return toCartResponse(cart);
        }

        /**
         * Funcion que calcula el subtotal del carrito a partir del precio unitario de
         * los items.
         * Es privada porque es una funcion interna de la clase y no necesita ser
         * expuesta a otras clases.
         * 
         * @param items
         * @return subtotal del carrito como BigDecimal
         */
        private BigDecimal calculateSubtotal(List<CartItem> items) {

                return items.stream()
                                .map(item -> item.getProduct().getPrice()
                                                .multiply(BigDecimal.valueOf(item.getQuantity())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Funciones de mapeo de entidades a DTOs

        /// Transforma un CartItem a un CartItemResponse
        private CartItemResponse toCartItemResponse(CartItem item) {

                CartItemResponse response = new CartItemResponse();

                response.setId(item.getId());
                response.setProductId(item.getProduct().getId());
                response.setProductName(item.getProduct().getName());
                response.setProductImages(item.getProduct().getImageUrls());
                response.setUnitPrice(item.getProduct().getPrice());
                response.setQuantity(item.getQuantity());

                response.setSubtotal(
                                item.getProduct().getPrice()
                                                .multiply(BigDecimal.valueOf(item.getQuantity())));

                return response;
        }

        /// Transforma un Cart a un CartResponse
        private CartResponse toCartResponse(Cart cart) {

                List<CartItem> items = cartItemRepository.findByCart_Id(cart.getId());

                CartResponse response = new CartResponse();

                response.setId(cart.getId());

                response.setItems(
                                items.stream()
                                                .map(item -> toCartItemResponse(item))
                                                .toList());

                response.setSubtotal(calculateSubtotal(items));

                return response;
        }

}
