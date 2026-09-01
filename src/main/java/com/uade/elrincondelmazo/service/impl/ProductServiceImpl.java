package com.uade.elrincondelmazo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.ProductRequest;
import com.uade.elrincondelmazo.enums.ProductStatus;
import com.uade.elrincondelmazo.enums.ProductType;
import com.uade.elrincondelmazo.enums.Role;
import com.uade.elrincondelmazo.exception.InvalidProductException;
import com.uade.elrincondelmazo.exception.ProductAccessDeniedException;
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

                if (request.getCollectionId() == null) {
                        throw new InvalidProductException(
                                        "La coleccion es obligatoria");
                }
        }

        @Override
        public Product createProduct(Long userId, ProductRequest request) {

                validateProduct(request);

                User seller = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Usuario no encontrado con id: " + userId));

                Collection collection = collectionRepository
                                .findById(request.getCollectionId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Coleccion no encontrada con id: "
                                                                + request.getCollectionId()));

                Product product = new Product();

                product.setName(request.getName());
                product.setDescription(request.getDescription());
                product.setPrice(request.getPrice());
                product.setType(request.getType());
                product.setImageUrls(request.getImageUrls());
                product.setStock(request.getStock());

                if (request.getStock() == 0) {
                        product.setStatus(ProductStatus.AGOTADO);
                } else {
                        product.setStatus(ProductStatus.ACTIVO);
                }
                product.setCreated_at(LocalDateTime.now());

                product.setSeller(seller);
                product.setCollection(collection);

                return productRepository.save(product);
        }

        @Override
        public Page<Product> getProducts(
                        String search,
                        ProductType type,
                        Long collectionId,
                        BigDecimal minPrice,
                        BigDecimal maxPrice,
                        PageRequest pageRequest) {

                if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
                        throw new InvalidProductException(
                                        "El precio minimo no puede ser negativo");
                }

                if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
                        throw new InvalidProductException(
                                        "El precio maximo no puede ser negativo");
                }

                if (minPrice != null && maxPrice != null
                                && minPrice.compareTo(maxPrice) > 0) {
                        throw new InvalidProductException(
                                        "El precio minimo no puede ser mayor al precio maximo");
                }

                if (search == null || search.isBlank()) {
                        search = "";
                }

                return productRepository.findProducts(
                                search,
                                type,
                                collectionId,
                                minPrice,
                                maxPrice,
                                pageRequest);
        }

        @Override
        public Product getProductById(Long id) {
                return productRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Producto no encontrado con id: " + id));
        }

        @Override
        public Page<Product> getProductsBySeller(
                        Long sellerId,
                        PageRequest pageRequest) {

                userRepository.findById(sellerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Usuario no encontrado con id: " + sellerId));

                return productRepository.findBySellerId(
                                sellerId,
                                pageRequest);
        }

        @Override
        public Product updateProduct(
                        Long userId,
                        Long id,
                        ProductRequest request) {

                validateProduct(request);

                Product product = getProductById(id);
                validateOwnerOrAdmin(product, userId);

                Collection collection = collectionRepository
                                .findById(request.getCollectionId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Coleccion no encontrada con id: "
                                                                + request.getCollectionId()));

                product.setName(request.getName());
                product.setDescription(request.getDescription());
                product.setPrice(request.getPrice());
                product.setType(request.getType());
                product.setImageUrls(request.getImageUrls());
                product.setStock(request.getStock());
                if (request.getStock() == 0) {
                        product.setStatus(ProductStatus.AGOTADO);
                } else if (product.getStatus() == ProductStatus.AGOTADO) {
                        product.setStatus(ProductStatus.ACTIVO);
                }
                product.setCollection(collection);

                return productRepository.save(product);
        }

        @Override
        public void deleteProduct(
                        Long userId,
                        Long id) {

                Product product = getProductById(id);

                validateOwnerOrAdmin(product, userId);

                productRepository.delete(product);
        }

        private void validateOwnerOrAdmin(
                        Product product,
                        Long userId) {

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Usuario no encontrado con id: " + userId));

                boolean isOwner = product.getSeller().getId().equals(userId);

                boolean isAdmin = user.getRole() == Role.ADMIN;

                if (!isOwner && !isAdmin) {
                        throw new ProductAccessDeniedException(
                                        "No tenes permiso para modificar este producto");
                }
        }
}