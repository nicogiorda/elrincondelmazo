package com.uade.elrincondelmazo.service.impl;

import com.uade.elrincondelmazo.entity.Order;
import com.uade.elrincondelmazo.entity.dto.OrderResponse;
import com.uade.elrincondelmazo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uade.elrincondelmazo.service.OrderService;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;

    }

    ///Decision importante: Si un usuario no tiene pedidos, se devuelve una lista vacia y no una excepcion
    ///Esto es para evitar manejar "El usuario no registra pedidos" como un error.
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
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

}
