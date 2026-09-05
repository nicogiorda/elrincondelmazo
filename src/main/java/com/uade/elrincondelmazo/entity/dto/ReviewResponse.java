package com.uade.elrincondelmazo.entity.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ReviewResponse {

    private Long id;

    private Long productId;

    private Long userId;
    private String userName;

    private Integer rating;
    private String comment;

    private LocalDateTime createdAt;
}