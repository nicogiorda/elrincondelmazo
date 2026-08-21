package com.uade.elrincondelmazo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.uade.elrincondelmazo.enums.ProductStatus;
import com.uade.elrincondelmazo.enums.ProductType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column
    private ProductType type;

    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", nullable = false)
    private List<String> imageUrls;

    @Column
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column
    private ProductStatus status;

    @Column
    private LocalDateTime created_at;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_id", nullable = false)
    private Collection collection;
}
