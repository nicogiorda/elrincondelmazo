package com.uade.elrincondelmazo.entity.dto.album;

import com.uade.elrincondelmazo.enums.AlbumSectionKey;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveAlbumCardRequest(
        @NotNull AlbumSectionKey section,
        @NotNull @Min(1) Integer pageNumber,
        @NotNull @Min(0) @Max(8) Integer slotIndex,
        boolean replaceExisting) {
}
