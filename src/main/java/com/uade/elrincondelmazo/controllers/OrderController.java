package com.uade.elrincondelmazo.controllers;

import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.CheckoutRequest;
import com.uade.elrincondelmazo.entity.dto.OrderResponse;
import com.uade.elrincondelmazo.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    ///Inyectamos las dependencias necesarias a través del constructor, en este caso el OrderService
    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /// Endpoint para obtener los pedidos del usuario.
    /// el usuario se obtiene a través de la anotación @AuthenticationPrincipal,
    /// que nos permite acceder al usuario autenticado en el contexto de seguridad de Spring Security.
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(
                orderService.getOrdersByUserId(user.getId())
        );
    }

    @PostMapping
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CheckoutRequest request) {

        OrderResponse order =
                orderService.checkout(
                        user.getId(),
                        request
                );

        URI location =
                URI.create("/orders/" + order.getId());

        return ResponseEntity
                .created(location)
                .body(order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) {

        return ResponseEntity.ok(
                orderService.getOrderById(
                        user.getId(),
                        orderId
                )
        );
    }

}