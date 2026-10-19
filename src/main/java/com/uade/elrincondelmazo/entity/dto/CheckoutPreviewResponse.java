package com.uade.elrincondelmazo.entity.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class CheckoutPreviewResponse {
    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal total;
    private List<AppliedPromotionResponse> promotions;
}
