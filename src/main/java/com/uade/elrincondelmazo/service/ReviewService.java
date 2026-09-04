package com.uade.elrincondelmazo.service;

import java.util.List;

import com.uade.elrincondelmazo.entity.dto.CreateReviewRequest;
import com.uade.elrincondelmazo.entity.dto.ReviewResponse;

public interface ReviewService {

    List<ReviewResponse> getReviewsByProductId(
            Long productId
    );

    ReviewResponse createReview(
            Long userId,
            Long productId,
            CreateReviewRequest request
    );
}