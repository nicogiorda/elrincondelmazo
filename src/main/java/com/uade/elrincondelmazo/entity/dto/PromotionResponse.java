package com.uade.elrincondelmazo.entity.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.uade.elrincondelmazo.enums.PaymentMethod;
import com.uade.elrincondelmazo.enums.ProductType;
import com.uade.elrincondelmazo.enums.PromotionType;

import lombok.Data;

@Data
public class PromotionResponse {

    /*
     * DTO usado para devolver información de una promoción al cliente.
     * Contiene los campos relevantes para mostrar la promoción y sus
     * condiciones (tipos, límites, relaciones a colección/producto,
     * vigencia y si es combinable).
     */

    private Long id;
    // Identificador de la promoción

    private String name; // Nombre legible de la promoción
    private String description; // Descripción para mostrar al usuario

    // Tipo y porcentaje de descuento
    private PromotionType type;
    private BigDecimal discountPercentage;

    // Condiciones (opcionales) que la promoción puede exigir
    private Integer minimumQuantity; // cantidad mínima si aplica
    private BigDecimal minimumAmount; // monto mínimo si aplica

    private ProductType productType; // tipo de producto objetivo (opcional)
    private PaymentMethod paymentMethod; // método de pago requerido (opcional)

    // Información de la colección objetivo (si aplica)
    private Long collectionId;
    private String collectionName;

    // Información del producto objetivo (si aplica)
    private Long productId;
    private String productName;

    // Indica si la promoción se puede combinar con otras
    private boolean stackable;

    // Vigencia de la promoción
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Flag que indica si la promoción está activa
    private boolean active;
}