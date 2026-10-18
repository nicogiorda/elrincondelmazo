package com.uade.elrincondelmazo.service;

import java.util.List;

import com.uade.elrincondelmazo.entity.CartItem;
import com.uade.elrincondelmazo.entity.dto.CreatePromotionRequest;
import com.uade.elrincondelmazo.entity.dto.PromotionApplicationResponse;
import com.uade.elrincondelmazo.entity.dto.PromotionResponse;
import com.uade.elrincondelmazo.entity.dto.UpdatePromotionRequest;
import com.uade.elrincondelmazo.enums.PaymentMethod;

public interface PromotionService {

    /*
     * Interfaz del servicio de promociones.
     * Define las operaciones disponibles para administrar promociones
     * y para aplicar las reglas de descuento sobre un carrito de compra.
     */

    // Devuelve una lista con todas las promociones (mapeadas a DTOs)
    List<PromotionResponse> getAllPromotions();

    // Obtiene una promoción por su id (lanza excepción en la implementación si no existe)
    PromotionResponse getPromotionById(Long id);

    // Crea una nueva promoción a partir del DTO de creación
    PromotionResponse createPromotion(CreatePromotionRequest request);

    // Actualiza una promoción existente con los datos del DTO de actualización
    PromotionResponse updatePromotion(Long id, UpdatePromotionRequest request);

    // Activa una promoción (marca como activa)
    PromotionResponse activatePromotion(Long id);

    // Desactiva una promoción (marca como inactiva)
    PromotionResponse deactivatePromotion(Long id);

    // Aplica las promociones vigentes al carrito de items y devuelve
    // el resultado con el descuento total y el desglose de promociones aplicadas
    PromotionApplicationResponse applyPromotions(List<CartItem> items, PaymentMethod paymentMethod);
}
