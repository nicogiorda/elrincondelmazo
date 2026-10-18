package com.uade.elrincondelmazo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.uade.elrincondelmazo.enums.PaymentMethod;
import com.uade.elrincondelmazo.enums.ProductType;
import com.uade.elrincondelmazo.enums.PromotionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Promotion {

    /*
     * Entidad que representa una promoción/descuento aplicable al carrito.
     * Campos principales:
     * - `type`: Tipo de promoción (cantidad, colección, producto, etc.)
     * - `discountPercentage`: porcentaje a aplicar sobre la base correspondiente
     * - Condiciones opcionales: minimumQuantity, minimumAmount, productType,
     *   paymentMethod, collection, product
     * - `stackable`: si puede combinarse con otras promociones
     * - `startDate` / `endDate`: período de vigencia
     * - `active`: marca si la promoción está habilitada
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador único auto-generado

    @Column(nullable = false)
    private String name;

    // Nombre y descripción legible para mostrar al cliente
    @Column
    private String description;

    // Tipo de promoción (enum). Determina la condición y la base del descuento.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionType type;

    // Porcentaje de descuento a aplicar (ej. 10 para 10%)
    @Column(name = "discount_percentage", nullable = false)
    private BigDecimal discountPercentage;

    // Condición opcional: cantidad mínima del carrito requerida
    @Column(name = "minimum_quantity")
    private Integer minimumQuantity;

    // Condición opcional: monto mínimo del carrito requerido
    @Column(name = "minimum_amount")
    private BigDecimal minimumAmount;

    // Condición opcional: tipo de producto al que aplica la promoción
    @Enumerated(EnumType.STRING)
    @Column(name = "product_type")
    private ProductType productType;

    // Condición opcional: método de pago requerido para aplicar la promoción
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    // Relación opcional a Collection: si se especifica, la promoción solo
    // calcula la base sobre los items de esa colección
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection;

    // Relación opcional a Product: si se especifica, la promoción solo
    // aplica sobre ese producto
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // Indica si la promoción puede combinarse con otras (stackable)
    @Column(nullable = false)
    private boolean stackable;

    // Período de vigencia: fecha de inicio (requerida)
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    // Fecha de fin de vigencia (opcional). Si es null la promoción no expira
    @Column(name = "end_date")
    private LocalDateTime endDate;

    // Flag que indica si la promoción está habilitada para aplicarse
    @Column(nullable = false)
    private boolean active;
}