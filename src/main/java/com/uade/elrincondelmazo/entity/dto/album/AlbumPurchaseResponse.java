package com.uade.elrincondelmazo.entity.dto.album;

/** An order is confirmed by checkout; it does NOT certify external payment. */
public record AlbumPurchaseResponse(
        Long orderItemId,
        Long orderId,
        String name,
        String imageUrl,
        int purchasedQuantity,
        long usedQuantity,
        long availableQuantity) {
}
