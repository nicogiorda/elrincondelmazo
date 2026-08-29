package com.uade.elrincondelmazo.controllers;

import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.AddCartItemRequest;
import com.uade.elrincondelmazo.entity.dto.CartResponse;
import com.uade.elrincondelmazo.entity.dto.UpdateCartItemQuantityRequest;
import com.uade.elrincondelmazo.service.CartService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carts")
public class CartController {

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(
                cartService.getOrCreateCart(user.getId())
        );
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddCartItemRequest request) {

        return ResponseEntity.ok(
                cartService.addItem(user.getId(), request)
        );
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @AuthenticationPrincipal User user,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {

        return ResponseEntity.ok(
                cartService.updateItemQuantity(
                        user.getId(),
                        cartItemId,
                        request
                )
        );
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long cartItemId) {

        return ResponseEntity.ok(
                cartService.removeItem(
                        user.getId(),
                        cartItemId
                )
        );
    }
}