package com.uade.elrincondelmazo.entity.dto.album;
import com.uade.elrincondelmazo.enums.*;
import java.util.List;
public record AlbumResponse(Long id, Long userId, AlbumVisibility visibility, List<SectionResponse> sections) {
    public record SectionResponse(Long id, AlbumSectionKey key, int pageCount, List<CardResponse> cards) {}
    public record CardResponse(Long id, int pageNumber, int slotIndex, String name, String imageUrl, AlbumCardOrigin origin) {}
}
