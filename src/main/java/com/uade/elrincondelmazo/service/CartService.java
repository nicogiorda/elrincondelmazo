package com.uade.elrincondelmazo.service;

import com.uade.elrincondelmazo.entity.dto.AddCartItemRequest;
import com.uade.elrincondelmazo.entity.dto.CartResponse;

public interface CartService {

    CartResponse getOrCreateCart(Long userId);

    CartResponse addItem(Long userId, AddCartItemRequest request);

}
