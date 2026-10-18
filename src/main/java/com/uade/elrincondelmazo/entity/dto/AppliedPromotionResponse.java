package com.uade.elrincondelmazo.entity.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppliedPromotionResponse {
    /*
     * DTO que representa una promoción que fue aplicada al carrito.
     * - `promotionId` / `promotionName`: identificador y nombre de la promoción
     * - `discountApplied`: monto de descuento que efectivamente se aplicó
     */

    private Long promotionId;
    private String promotionName;
    private BigDecimal discountApplied;
}