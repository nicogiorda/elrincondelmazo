package com.uade.elrincondelmazo.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.uade.elrincondelmazo.enums.ProductStatus;

import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.enums.ProductType;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            SELECT p FROM Product p
            WHERE p.status IN :visibleStatuses
            AND (:search IS NULL
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:type IS NULL OR p.type = :type)
            AND (:collectionId IS NULL OR p.collection.id = :collectionId)
            AND (:minPrice IS NULL OR p.price >= :minPrice)
            AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            """)
    Page<Product> findProducts(
            @Param("search") String search,
            @Param("type") ProductType type,
            @Param("collectionId") Long collectionId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("visibleStatuses") List<ProductStatus> visibleStatuses,
            Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.seller.id = :sellerId AND p.status <> :excludedStatus
            ORDER BY p.created_at DESC, p.id DESC
            """)
    Page<Product> findOwnPublications(
            @Param("sellerId") Long sellerId,
            @Param("excludedStatus") ProductStatus excludedStatus,
            Pageable pageable);

    Page<Product> findBySellerId(
            Long sellerId,
            Pageable pageable);
}