package com.uade.elrincondelmazo.entity.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class PromotionApplicationResponse {

    private BigDecimal totalDiscount;
    private List<AppliedPromotionResponse> appliedPromotions;
}