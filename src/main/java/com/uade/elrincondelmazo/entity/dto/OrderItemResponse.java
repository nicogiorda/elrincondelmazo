package com.uade.elrincondelmazo.entity.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemResponse {

    private Long id;

    private Long productId;
    private String productName;

    private Long sellerId;
    private String sellerEmail;

    private Integer quantity;

    private BigDecimal price;

}
