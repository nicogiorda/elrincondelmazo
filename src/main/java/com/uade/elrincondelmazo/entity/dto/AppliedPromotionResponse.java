package com.uade.elrincondelmazo.entity.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppliedPromotionResponse {

    private Long promotionId;
    private String promotionName;
    private BigDecimal discountApplied;
}