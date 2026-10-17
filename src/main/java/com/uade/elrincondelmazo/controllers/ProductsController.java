package com.uade.elrincondelmazo.controllers;

import java.math.BigDecimal;
import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.ImageUploadResponse;
import com.uade.elrincondelmazo.entity.dto.ProductRequest;
import com.uade.elrincondelmazo.entity.dto.ProductResponse;
import com.uade.elrincondelmazo.enums.ProductType;
import com.uade.elrincondelmazo.exception.InvalidProductException;
import com.uade.elrincondelmazo.service.ImageStorageService;
import com.uade.elrincondelmazo.service.ProductService;

@RestController
@RequestMapping("/products")
public class ProductsController {

        @Autowired
        private ProductService productService;
        @Autowired
        private ImageStorageService imageStorageService;

        @GetMapping
        public ResponseEntity<Page<ProductResponse>> getProducts(
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) ProductType type,
                        @RequestParam(required = false) Long collectionId,
                        @RequestParam(required = false) BigDecimal minPrice,
                        @RequestParam(required = false) BigDecimal maxPrice,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                if (page < 0) {
                        throw new InvalidProductException(
                                        "El numero de pagina no puede ser negativo");
                }

                if (size <= 0) {
                        throw new InvalidProductException(
                                        "El tamaño de pagina debe ser mayor a cero");
                }

                PageRequest pageRequest = PageRequest.of(page, size);

                Page<ProductResponse> products = productService
                                .getProducts(
                                                search,
                                                type,
                                                collectionId,
                                                minPrice,
                                                maxPrice,
                                                pageRequest)
                                .map(ProductResponse::fromProduct);

                return ResponseEntity.ok(products);
        }

        /** Todas las publicaciones propias salvo eliminadas, con paginación SQL.
         * page >= 0; size entre 1 y 200 (20 por defecto). Orden: created_at DESC, id DESC.
         * El límite es por página, no por historial.
         */
        @GetMapping("/mine")
        public ResponseEntity<Page<ProductResponse>> getOwnPublications(
                        @AuthenticationPrincipal User user,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                if (page < 0 || size < 1 || size > 200) {
                        throw new InvalidProductException("La pagina debe ser mayor o igual a cero y el tamaño debe estar entre 1 y 200");
                }
                return ResponseEntity.ok(productService
                                .getOwnPublications(user.getId(), PageRequest.of(page, size))
                                .map(ProductResponse::fromProduct));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ProductResponse> getProductById(
                        @PathVariable Long id) {

                Product product = productService.getProductById(id);

                return ResponseEntity.ok(
                                ProductResponse.fromProduct(product));
        }

        @PostMapping
        public ResponseEntity<ProductResponse> createProduct(
                        @AuthenticationPrincipal User user,
                        @RequestBody ProductRequest productRequest) {

                Product product = productService.createProduct(
                                user.getId(),
                                productRequest);

                ProductResponse response = ProductResponse.fromProduct(product);

                URI location = URI.create("/products/" + product.getId());

                return ResponseEntity
                                .created(location)
                                .body(response);
        }

        @PutMapping("/{id}")
        public ResponseEntity<ProductResponse> updateProduct(
                        @AuthenticationPrincipal User user,
                        @PathVariable Long id,
                        @RequestBody ProductRequest productRequest) {

                Product product = productService.updateProduct(
                                user.getId(),
                                id,
                                productRequest);

                return ResponseEntity.ok(
                                ProductResponse.fromProduct(product));
        }

        /** Eliminación lógica idempotente, compatible con DELETE de main.
         * Se retira el antiguo PUT /{id}/deactivate: no tiene consumidores en el frontend.
         */
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteProduct(
                        @AuthenticationPrincipal User user,
                        @PathVariable Long id) {
                productService.deleteProduct(user.getId(), id);
                return ResponseEntity.noContent().build();
        }

        /** ACTIVO -> INACTIVO; repetir INACTIVO devuelve 200 sin cambios.
         * AGOTADO devuelve 400; ELIMINADO o inexistente devuelve 404.
         */
        @PatchMapping("/{id}/pause")
        public ResponseEntity<ProductResponse> pauseProduct(
                        @AuthenticationPrincipal User user,
                        @PathVariable Long id) {
                return ResponseEntity.ok(ProductResponse.fromProduct(productService.pauseProduct(user.getId(), id)));
        }

        /** INACTIVO -> ACTIVO con stock positivo. ACTIVO con stock: 200 sin cambios.
         * Sin stock u otro estado no reactivable: 400; ELIMINADO o inexistente: 404.
         */
        @PatchMapping("/{id}/resume")
        public ResponseEntity<ProductResponse> resumeProduct(
                        @AuthenticationPrincipal User user,
                        @PathVariable Long id) {
                return ResponseEntity.ok(ProductResponse.fromProduct(productService.resumeProduct(user.getId(), id)));
        }

        @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ImageUploadResponse> uploadImage(
                        @AuthenticationPrincipal User user,
                        @RequestParam("file") MultipartFile file) {

                String url = imageStorageService
                                .uploadProductImage(
                                                user.getId(),
                                                file);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(
                                                new ImageUploadResponse(url));
        }
}