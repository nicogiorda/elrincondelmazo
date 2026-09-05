package com.uade.elrincondelmazo.entity.dto;

import com.uade.elrincondelmazo.enums.PaymentMethod;
import com.uade.elrincondelmazo.enums.ProductType;
import com.uade.elrincondelmazo.enums.PromotionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdatePromotionRequest {

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

    @Positive
    private Long productId;

    @NotNull
    private Boolean stackable;

    @NotNull
    private LocalDateTime startDate;

    private LocalDateTime endDate;
}