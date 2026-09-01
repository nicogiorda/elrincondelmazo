package com.uade.elrincondelmazo.entity.dto;

import com.uade.elrincondelmazo.enums.PaymentMethod;
import com.uade.elrincondelmazo.enums.StateOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
//Representa una orden completa
public class OrderResponse {

    private Long id;

    private LocalDateTime orderDate;
    private StateOrder state;
    private PaymentMethod paymentMethod;

    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal total;

    private List<OrderItemResponse> items;
    private List<OrderPromotionResponse> promotions;


}
