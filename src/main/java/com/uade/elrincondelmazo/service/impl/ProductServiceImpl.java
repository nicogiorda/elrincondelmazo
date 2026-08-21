package com.uade.elrincondelmazo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.ProductRequest;
import com.uade.elrincondelmazo.enums.ProductStatus;
import com.uade.elrincondelmazo.exception.InvalidProductException;
import com.uade.elrincondelmazo.exception.ResourceNotFoundException;
import com.uade.elrincondelmazo.repository.CollectionRepository;
import com.uade.elrincondelmazo.repository.ProductRepository;
import com.uade.elrincondelmazo.repository.UserRepository;
import com.uade.elrincondelmazo.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    private void validateProduct(ProductRequest request) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new InvalidProductException("El nombre del producto es obligatorio");
        }

        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new InvalidProductException("La descripcion del producto es obligatoria");
        }

        if (request.getPrice() == null ||
                request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidProductException("El precio debe ser mayor a cero");
        }

        if (request.getType() == null) {
            throw new InvalidProductException("El tipo de producto es obligatorio");
        }

        if (request.getImageUrls() == null || request.getImageUrls().isEmpty()) {
            throw new InvalidProductException(
                    "El producto debe tener al menos una imagen");
        }

        if (request.getStock() == null || request.getStock() < 0) {
            throw new InvalidProductException(
                    "El stock no puede ser negativo");
        }

        if (request.getSellerId() == null) {
            throw new InvalidProductException(
                    "El vendedor es obligatorio");
        }

        if (request.getCollectionId() == null) {
            throw new InvalidProductException(
                    "La coleccion es obligatoria");
        }
    }

    @Override
    public Product createProduct(ProductRequest request) {

        validateProduct(request);

        User seller = userRepository.findById(request.getSellerId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vendedor no encontrado con id: "
                                        + request.getSellerId()
                        )
                );

        Collection collection = collectionRepository
                .findById(request.getCollectionId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Coleccion no encontrada con id: "
                                        + request.getCollectionId()
                        )
                );

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setType(request.getType());
        product.setImageUrls(request.getImageUrls());
        product.setStock(request.getStock());

        product.setStatus(ProductStatus.ACTIVO);
        product.setCreated_at(LocalDateTime.now());

        product.setSeller(seller);
        product.setCollection(collection);

        return productRepository.save(product);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }

    @Override
    public Product updateProduct(
        Long id,
        ProductRequest request) {

    validateProduct(request);

    Product product = getProductById(id);

    User seller = userRepository.findById(request.getSellerId())
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Vendedor no encontrado con id: "
                                    + request.getSellerId()
                    )
            );

    Collection collection = collectionRepository
            .findById(request.getCollectionId())
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Coleccion no encontrada con id: "
                                    + request.getCollectionId()
                    )
            );

    product.setName(request.getName());
    product.setDescription(request.getDescription());
    product.setPrice(request.getPrice());
    product.setType(request.getType());
    product.setImageUrls(request.getImageUrls());
    product.setStock(request.getStock());
    product.setSeller(seller);
    product.setCollection(collection);

    return productRepository.save(product);
    }
    
    @Override
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }
}