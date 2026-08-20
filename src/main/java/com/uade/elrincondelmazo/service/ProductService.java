package com.uade.elrincondelmazo.service;

import java.util.List;

import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.entity.dto.ProductRequest;

public interface ProductService {

    List<Product> getAllProducts();

    Product getProductById(Long id);

    Product createProduct(ProductRequest productRequest); //Post

    Product updateProduct(Long id, ProductRequest productRequest); //Put

    void deleteProduct(Long id);
}