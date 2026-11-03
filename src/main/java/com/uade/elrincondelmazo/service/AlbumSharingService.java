package com.uade.elrincondelmazo.service;

import com.uade.elrincondelmazo.entity.dto.album.AlbumResponse;
import com.uade.elrincondelmazo.enums.AlbumVisibility;

public interface AlbumSharingService {
    AlbumResponse viewAlbum(Long viewerId, Long ownerId);
    AlbumResponse setVisibility(Long ownerId, AlbumVisibility visibility);
}
