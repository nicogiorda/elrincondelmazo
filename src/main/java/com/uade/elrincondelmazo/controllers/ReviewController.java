package com.uade.elrincondelmazo.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.CreateReviewRequest;
import com.uade.elrincondelmazo.entity.dto.ReviewResponse;
import com.uade.elrincondelmazo.service.ReviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/products/{productId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(
            ReviewService reviewService) {

        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(
            @PathVariable Long productId) {

        return ResponseEntity.ok(
                reviewService
                        .getReviewsByProductId(productId)
        );
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId,
            @Valid @RequestBody CreateReviewRequest request) {

        ReviewResponse review =
                reviewService.createReview(
                        user.getId(),
                        productId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(review);
    }
}