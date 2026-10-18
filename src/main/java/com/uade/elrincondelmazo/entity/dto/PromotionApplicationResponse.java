package com.uade.elrincondelmazo.entity.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class PromotionApplicationResponse {

    /*
     * DTO que representa el resultado de aplicar promociones al carrito.
     * - `totalDiscount`: monto total descontado del subtotal
     * - `appliedPromotions`: lista con el desglose de promociones aplicadas
     */

    private BigDecimal totalDiscount;
    private List<AppliedPromotionResponse> appliedPromotions;
}