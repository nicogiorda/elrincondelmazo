package com.uade.elrincondelmazo.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


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
