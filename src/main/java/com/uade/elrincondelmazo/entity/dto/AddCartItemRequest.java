package com.uade.elrincondelmazo.entity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AddCartItemRequest {

    @NotNull
    private Long productId;

    @NotNull
    @Positive
    private Integer quantity;
}