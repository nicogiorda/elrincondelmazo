package com.uade.elrincondelmazo.service;

import com.uade.elrincondelmazo.entity.dto.AddCartItemRequest;
import com.uade.elrincondelmazo.entity.dto.CartResponse;
import com.uade.elrincondelmazo.entity.dto.UpdateCartItemQuantityRequest;

public interface CartService {

    CartResponse getOrCreateCart(Long userId);

    CartResponse addItem(Long userId, AddCartItemRequest request);

    CartResponse updateItemQuantity(
            Long userId,
            Long cartItemId,
            UpdateCartItemQuantityRequest request
    );

    CartResponse removeItem(Long userId, Long cartItemId);

}
