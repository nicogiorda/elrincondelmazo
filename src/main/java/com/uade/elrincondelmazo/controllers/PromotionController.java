package com.uade.elrincondelmazo.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uade.elrincondelmazo.entity.dto.CreatePromotionRequest;
import com.uade.elrincondelmazo.entity.dto.PromotionResponse;
import com.uade.elrincondelmazo.entity.dto.UpdatePromotionRequest;
import com.uade.elrincondelmazo.service.PromotionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/promotions")
public class PromotionController {
        // Controlador REST que expone endpoints para administrar promociones
        // Rutas base: /promotions

        @Autowired
        private PromotionService promotionService;

    @GetMapping("/public")
    public ResponseEntity<List<PromotionResponse>> getPublicPromotions() {
        return ResponseEntity.ok(promotionService.getPublicPromotions());
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> getAllPromotions() {

                // Devuelve todas las promociones (HTTP 200)
                return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponse> getPromotionById(
            @PathVariable Long id) {

        // Devuelve una promoción por id (HTTP 200). Si no existe, la capa
        // de servicio lanzará una excepción manejada por el controlador global.
        return ResponseEntity.ok(promotionService.getPromotionById(id));
    }

    @PostMapping
    public ResponseEntity<PromotionResponse> createPromotion(
            @Valid @RequestBody CreatePromotionRequest request) {

        // Crea una nueva promoción. Valida el DTO con @Valid.
        PromotionResponse promotion = promotionService.createPromotion(request);

        // Construye la URI de la nueva entidad y responde 201 Created
        URI location = URI.create("/promotions/" + promotion.getId());

        return ResponseEntity.created(location).body(promotion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponse> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePromotionRequest request) {

        // Actualiza la promoción indicada y devuelve el recurso actualizado
        return ResponseEntity.ok(promotionService.updatePromotion(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<PromotionResponse> activatePromotion(
            @PathVariable Long id) {

        // Marca la promoción como activa y devuelve la entidad actualizada
        return ResponseEntity.ok(promotionService.activatePromotion(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<PromotionResponse> deactivatePromotion(
            @PathVariable Long id) {

                // Marca la promoción como inactiva y devuelve la entidad actualizada
                return ResponseEntity.ok(promotionService.deactivatePromotion(id));
    }
}
