package com.uade.elrincondelmazo.entity.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UpdateCartItemQuantityRequest {

    @Positive(message = "La cantidad debe ser un número positivo")
    private Integer quantity;
}
