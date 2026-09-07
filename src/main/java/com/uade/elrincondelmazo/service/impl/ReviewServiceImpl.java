package com.uade.elrincondelmazo.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.entity.Review;
import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.CreateReviewRequest;
import com.uade.elrincondelmazo.entity.dto.ReviewResponse;
import com.uade.elrincondelmazo.enums.StateOrder;
import com.uade.elrincondelmazo.exception.ResourceNotFoundException;
import com.uade.elrincondelmazo.exception.ReviewAlreadyExistsException;
import com.uade.elrincondelmazo.exception.ReviewNotAllowedException;
import com.uade.elrincondelmazo.repository.OrderItemRepository;
import com.uade.elrincondelmazo.repository.ProductRepository;
import com.uade.elrincondelmazo.repository.ReviewRepository;
import com.uade.elrincondelmazo.repository.UserRepository;
import com.uade.elrincondelmazo.service.ReviewService;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Autowired
    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderItemRepository orderItemRepository) {

        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public List<ReviewResponse> getReviewsByProductId(
            Long productId) {

        productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado con id: "
                                        + productId));

        return reviewRepository
                .findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ReviewResponse createReview(
            Long userId,
            Long productId,
            CreateReviewRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario no encontrado con id: "
                                        + userId));

        Product product = productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Producto no encontrado con id: "
                                        + productId));

        boolean purchased =
                orderItemRepository
                        .existsByOrder_User_IdAndProduct_IdAndOrder_StateNot(
                                userId,
                                productId,
                                StateOrder.CANCELADO
                        );

        if (!purchased) {
            throw new ReviewNotAllowedException(
                    "Solo se puede reseñar un producto comprado"
            );
        }

        if (reviewRepository
                .existsByUserIdAndProductId(
                        userId,
                        productId)) {

            throw new ReviewAlreadyExistsException(
                    "El usuario ya realizó una reseña para este producto"
            );
        }

        Review review = new Review();

        review.setUser(user);
        review.setProduct(product);
        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());
        review.setCreatedAt(LocalDateTime.now());

        review = reviewRepository.save(review);

        return toResponse(review);
    }

    private ReviewResponse toResponse(Review review) {

        ReviewResponse response = new ReviewResponse();

        response.setId(review.getId());

        response.setProductId(
                review.getProduct().getId());

        response.setUserId(
                review.getUser().getId());

        response.setUserName(
                review.getUser().getFirstName()
                        + " "
                        + review.getUser().getLastName());

        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());

        return response;
    }
}