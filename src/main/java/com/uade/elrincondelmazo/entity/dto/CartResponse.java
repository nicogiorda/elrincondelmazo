package com.uade.elrincondelmazo.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartResponse {

    private Long id;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;

}
