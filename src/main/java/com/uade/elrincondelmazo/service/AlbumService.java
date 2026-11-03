package com.uade.elrincondelmazo.service;

import com.uade.elrincondelmazo.entity.dto.album.AlbumResponse;
import com.uade.elrincondelmazo.entity.dto.album.MoveAlbumCardRequest;

public interface AlbumService {
    AlbumResponse getMine(Long userId);
    AlbumResponse.CardResponse moveCard(Long userId, Long cardId, MoveAlbumCardRequest request);
    void removeCard(Long userId, Long cardId);
}
