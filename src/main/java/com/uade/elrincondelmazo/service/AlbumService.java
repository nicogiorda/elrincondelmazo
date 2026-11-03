package com.uade.elrincondelmazo.service;
import com.uade.elrincondelmazo.entity.dto.album.AlbumResponse;
public interface AlbumService {
    AlbumResponse getMine(Long userId);
}
