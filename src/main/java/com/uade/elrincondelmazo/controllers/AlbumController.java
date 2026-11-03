package com.uade.elrincondelmazo.controllers;

import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.entity.dto.album.AlbumResponse;
import com.uade.elrincondelmazo.entity.dto.album.MoveAlbumCardRequest;
import com.uade.elrincondelmazo.entity.dto.album.PurchaseAlbumCardRequest;
import com.uade.elrincondelmazo.entity.dto.album.AlbumPurchaseResponse;
import com.uade.elrincondelmazo.enums.AlbumSectionKey;
import com.uade.elrincondelmazo.service.AlbumCardRegistrationService;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;
import java.util.List;
import com.uade.elrincondelmazo.service.AlbumService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/albums")
public class AlbumController {
    private final AlbumService albumService;
    private final AlbumCardRegistrationService registration;

    public AlbumController(AlbumService albumService, AlbumCardRegistrationService registration) {
        this.albumService = albumService;
        this.registration = registration;
    }

    @GetMapping("/me")
    public ResponseEntity<AlbumResponse> getMine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(albumService.getMine(user.getId()));
    }

    @GetMapping("/me/purchases")
    public ResponseEntity<List<AlbumPurchaseResponse>> purchases(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(registration.availablePurchases(user.getId()));
    }

    @PostMapping("/me/cards/purchase")
    public ResponseEntity<AlbumResponse.CardResponse> addPurchase(
            @AuthenticationPrincipal User user, @Valid @RequestBody PurchaseAlbumCardRequest request) {
        AlbumResponse.CardResponse card = registration.addPurchase(user.getId(), request);
        return ResponseEntity.created(URI.create("/albums/me/cards/" + card.id())).body(card);
    }

    @PostMapping(value = "/me/cards/manual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AlbumResponse.CardResponse> addManual(
            @AuthenticationPrincipal User user,
            @RequestParam AlbumSectionKey section,
            @RequestParam int pageNumber,
            @RequestParam int slotIndex,
            @RequestParam String name,
            @RequestPart("image") MultipartFile image) {
        AlbumResponse.CardResponse card = registration.addManual(
                user.getId(), section, pageNumber, slotIndex, name, image);
        return ResponseEntity.created(URI.create("/albums/me/cards/" + card.id())).body(card);
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
