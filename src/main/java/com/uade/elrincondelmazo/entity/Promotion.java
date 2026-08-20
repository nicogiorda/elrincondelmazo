package com.uade.elrincondelmazo.entity;


import com.uade.elrincondelmazo.enums.PaymentMethod;
import com.uade.elrincondelmazo.enums.ProductType;
import com.uade.elrincondelmazo.enums.PromotionType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Data
public class Promotion {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PromotionType type;

    @Column(nullable = false)
    private Double discountPercentage;

    @Column
    private BigDecimal minimumAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private Boolean stackable;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column
    private LocalDateTime endDate;

    @Column
    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    ///No usamos referencedColumn = Id porque ya apunta al ID de collecion,
    ///hibernate asume automaticamente que se esta referenciando a la clave primaria de esa entidad por lo que seria redundante
    @JoinColumn(name = "coleccion_id")
    private Collection collection;
}

