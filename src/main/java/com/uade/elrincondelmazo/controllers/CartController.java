package com.uade.elrincondelmazo.controllers;

import com.uade.elrincondelmazo.entity.dto.AddCartItemRequest;
import com.uade.elrincondelmazo.entity.dto.CartResponse;
import com.uade.elrincondelmazo.entity.dto.UpdateCartItemQuantityRequest;
import com.uade.elrincondelmazo.service.CartService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                cartService.getOrCreateCart(userId)
        );
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable Long userId,
            @Valid @RequestBody AddCartItemRequest request) {

        return ResponseEntity.ok(
                cartService.addItem(userId, request)
        );
    }

    @PutMapping("/{userId}/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {

        return ResponseEntity.ok(
                cartService.updateItemQuantity(
                        userId,
                        cartItemId,
                        request
                )
        );
    }

    @DeleteMapping("/{userId}/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable Long userId,
            @PathVariable Long cartItemId) {

        return ResponseEntity.ok(
                cartService.removeItem(userId, cartItemId)
        );
    }
}