package com.uade.elrincondelmazo.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.entity.dto.ProductRequest;
import com.uade.elrincondelmazo.enums.ProductType;

public interface ProductService {

        Page<Product> getProducts(
                        String search,
                        ProductType type,
                        Long collectionId,
                        BigDecimal minPrice,
                        BigDecimal maxPrice,
                        PageRequest pageRequest);

        Product getProductById(Long id);

        Page<Product> getProductsBySeller(
                Long sellerId,
                PageRequest pageRequest);

        Product createProduct(
                        Long userId,
                        ProductRequest productRequest);

        Product updateProduct(
                        Long userId,
                        Long id,
                        ProductRequest productRequest);

        void deleteProduct(
                        Long userId,
                        Long id);

}
