package com.uade.elrincondelmazo.service;

import java.util.List;

import com.uade.elrincondelmazo.entity.CartItem;
import com.uade.elrincondelmazo.entity.dto.CreatePromotionRequest;
import com.uade.elrincondelmazo.entity.dto.PromotionApplicationResponse;
import com.uade.elrincondelmazo.entity.dto.PromotionResponse;
import com.uade.elrincondelmazo.entity.dto.UpdatePromotionRequest;
import com.uade.elrincondelmazo.enums.PaymentMethod;

public interface PromotionService {

    List<PromotionResponse> getAllPromotions();

    PromotionResponse getPromotionById(Long id);

    PromotionResponse createPromotion(
            CreatePromotionRequest request);

    PromotionResponse updatePromotion(
            Long id,
            UpdatePromotionRequest request);

    PromotionResponse activatePromotion(Long id);

    PromotionResponse deactivatePromotion(Long id);

    PromotionApplicationResponse applyPromotions(
            List<CartItem> items,
            PaymentMethod paymentMethod);
}

    



