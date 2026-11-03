package com.uade.elrincondelmazo.controllers;

import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.album.AlbumResponse;
import com.uade.elrincondelmazo.entity.dto.album.MoveAlbumCardRequest;
import com.uade.elrincondelmazo.service.AlbumService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/albums")
public class AlbumController {
    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @GetMapping("/me")
    public ResponseEntity<AlbumResponse> getMine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(albumService.getMine(user.getId()));
    }

    @PatchMapping("/me/cards/{cardId}")
    public ResponseEntity<AlbumResponse.CardResponse> moveCard(
            @AuthenticationPrincipal User user,
            @PathVariable Long cardId,
            @Valid @RequestBody MoveAlbumCardRequest request) {
        return ResponseEntity.ok(albumService.moveCard(user.getId(), cardId, request));
    }

    @DeleteMapping("/me/cards/{cardId}")
    public ResponseEntity<Void> removeCard(
            @AuthenticationPrincipal User user,
            @PathVariable Long cardId) {
        albumService.removeCard(user.getId(), cardId);
        return ResponseEntity.noContent().build();
    }
}
