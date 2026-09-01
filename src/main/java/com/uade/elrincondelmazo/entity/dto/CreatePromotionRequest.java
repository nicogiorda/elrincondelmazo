package com.uade.elrincondelmazo.entity.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.uade.elrincondelmazo.enums.PaymentMethod;
import com.uade.elrincondelmazo.enums.ProductType;
import com.uade.elrincondelmazo.enums.PromotionType;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreatePromotionRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private PromotionType type;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax("100.0")
    private BigDecimal discountPercentage;

    @Positive
    private Integer minimumQuantity;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal minimumAmount;

    private ProductType productType;

    private PaymentMethod paymentMethod;

    @Positive
    private Long collectionId;

    @NotNull
    private Boolean stackable;

    @NotNull
    private LocalDateTime startDate;

    private LocalDateTime endDate;
}