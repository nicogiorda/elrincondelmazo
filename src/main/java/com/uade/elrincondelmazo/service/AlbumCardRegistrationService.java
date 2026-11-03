package com.uade.elrincondelmazo.service;

import com.uade.elrincondelmazo.entity.dto.album.AlbumPurchaseResponse;
import com.uade.elrincondelmazo.entity.dto.album.AlbumResponse;
import com.uade.elrincondelmazo.entity.dto.album.PurchaseAlbumCardRequest;
import com.uade.elrincondelmazo.enums.AlbumSectionKey;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface AlbumCardRegistrationService {
    List<AlbumPurchaseResponse> availablePurchases(Long userId);
    AlbumResponse.CardResponse addPurchase(Long userId, PurchaseAlbumCardRequest request);
    AlbumResponse.CardResponse addManual(Long userId, AlbumSectionKey section,
                                        int pageNumber, int slotIndex, String name, MultipartFile image);
}
