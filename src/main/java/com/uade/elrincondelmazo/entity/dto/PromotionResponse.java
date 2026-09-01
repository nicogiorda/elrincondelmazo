package com.uade.elrincondelmazo.entity.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.uade.elrincondelmazo.enums.PaymentMethod;
import com.uade.elrincondelmazo.enums.ProductType;
import com.uade.elrincondelmazo.enums.PromotionType;

import lombok.Data;

@Data
public class PromotionResponse {

    private Long id;

    private String name;
    private String description;

    private PromotionType type;
    private BigDecimal discountPercentage;

    private Integer minimumQuantity;
    private BigDecimal minimumAmount;

    private ProductType productType;
    private PaymentMethod paymentMethod;

    private Long collectionId;
    private String collectionName;

    private boolean stackable;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private boolean active;
}