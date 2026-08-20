package com.uade.elrincondelmazo.entity.dto;

import java.math.BigDecimal;
import java.util.List;

import com.uade.elrincondelmazo.enums.ProductType;

import lombok.Data;

@Data
public class ProductRequest {

    private String name;

    private String description;

    private BigDecimal price;

    private ProductType type;

    private List<String> imageUrls;

    private Integer stock;

    private Long sellerId;

    private Long collectionId;
}