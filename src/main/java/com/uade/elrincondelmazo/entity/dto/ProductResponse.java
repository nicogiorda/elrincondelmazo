package com.uade.elrincondelmazo.entity.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.enums.ProductStatus;
import com.uade.elrincondelmazo.enums.ProductType;

import lombok.Data;
//**
//  Product referencia directamente a User.
// Si devolviéramos toda la entidad, podríamos terminar serializando datos del usuario que no queremos exponer.
// Con este DTO nosotros decidimos exactamente qué sale por la API.
//  */

@Data
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private ProductType type;
    private List<String> imageUrls;
    private Integer stock;
    private ProductStatus status;
    private LocalDateTime createdAt;

    private Long sellerId;

    private Long collectionId;
    private String collectionName;

    public static ProductResponse fromProduct(Product product) {

        ProductResponse response = new ProductResponse();

        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setType(product.getType());
        response.setImageUrls(product.getImageUrls());
        response.setStock(product.getStock());
        response.setStatus(product.getStatus());
        response.setCreatedAt(product.getCreated_at());

        response.setSellerId(product.getSeller().getId());

        response.setCollectionId(product.getCollection().getId());
        response.setCollectionName(product.getCollection().getName());

        return response;
    }
}
