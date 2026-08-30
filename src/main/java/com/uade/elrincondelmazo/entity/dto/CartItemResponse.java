package com.uade.elrincondelmazo.entity.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class CartItemResponse {

    private Long id;

    private Long productId;
    private String productName;
    private List<String> productImages;

    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;

}
