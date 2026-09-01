package com.uade.elrincondelmazo.service;

import com.uade.elrincondelmazo.entity.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    List<OrderResponse> getOrdersByUserId(Long userId);
}
