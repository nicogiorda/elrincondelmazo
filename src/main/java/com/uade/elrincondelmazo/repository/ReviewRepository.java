package com.uade.elrincondelmazo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uade.elrincondelmazo.entity.Review;

@Repository
public interface ReviewRepository
        extends JpaRepository<Review, Long> {

    List<Review> findByProductIdOrderByCreatedAtDesc(
            Long productId
    );

    boolean existsByUserIdAndProductId(
            Long userId,
            Long productId
    );
}