package com.uade.elrincondelmazo.service;

import com.uade.elrincondelmazo.entity.dto.CheckoutRequest;
import com.uade.elrincondelmazo.entity.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    List<OrderResponse> getOrdersByUserId(Long userId);

    com.uade.elrincondelmazo.entity.dto.CheckoutPreviewResponse preview(Long userId, CheckoutRequest request);

    OrderResponse checkout(Long userId, CheckoutRequest request);

    OrderResponse getOrderById(Long userId, Long orderId);

}
