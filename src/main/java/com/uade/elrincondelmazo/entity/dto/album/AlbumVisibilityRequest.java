package com.uade.elrincondelmazo.entity.dto.album;

import com.uade.elrincondelmazo.enums.AlbumVisibility;
import jakarta.validation.constraints.NotNull;

public record AlbumVisibilityRequest(@NotNull AlbumVisibility visibility) {
}
