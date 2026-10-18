package com.uade.elrincondelmazo.entity.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.uade.elrincondelmazo.enums.PaymentMethod;
import com.uade.elrincondelmazo.enums.ProductType;
import com.uade.elrincondelmazo.enums.PromotionType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreatePromotionRequest {

    /*
     * DTO usado para la creación de promociones desde la API.
     * Contiene validaciones (anotaciones de Jakarta Validation) para garantizar
     * que los campos mínimos estén presentes y con formatos válidos.
     */

    @NotBlank
    private String name;


    // Nombre de la promoción (obligatorio)
    private String description;

    @NotNull
    private PromotionType type;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)

    // Porcentaje de descuento (0 < porcentaje <= 100)
    @DecimalMax("100.0")
    private BigDecimal discountPercentage;

    // Cantidad mínima requerida (opcional, aplica para promociones por cantidad)

    @Positive

    // Monto mínimo requerido (opcional, aplica para promociones por monto)
    private Integer minimumQuantity;

    // Tipo de producto objetivo (opcional)


    // Método de pago requerido (opcional)
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal minimumAmount;

    // ID de la colección objetivo (opcional)

    private ProductType productType;

    // ID del producto objetivo (opcional)

    private PaymentMethod paymentMethod;

    // Indica si la promoción puede combinarse con otras (obligatorio)

    @Positive

    // Fecha de inicio obligatoria
    private Long collectionId;

    // Fecha de fin opcional (si es null la promoción no expira)

    @Positive
    private Long productId;

    @NotNull
    private Boolean stackable;

    @NotNull
    private LocalDateTime startDate;

    private LocalDateTime endDate;
}