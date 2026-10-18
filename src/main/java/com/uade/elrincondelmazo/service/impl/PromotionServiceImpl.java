package com.uade.elrincondelmazo.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uade.elrincondelmazo.entity.CartItem;
import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.entity.Promotion;
import com.uade.elrincondelmazo.entity.dto.AppliedPromotionResponse;
import com.uade.elrincondelmazo.entity.dto.CreatePromotionRequest;
import com.uade.elrincondelmazo.entity.dto.PromotionApplicationResponse;
import com.uade.elrincondelmazo.entity.dto.PromotionResponse;
import com.uade.elrincondelmazo.entity.dto.UpdatePromotionRequest;
import com.uade.elrincondelmazo.enums.PaymentMethod;
import com.uade.elrincondelmazo.enums.ProductType;
import com.uade.elrincondelmazo.enums.PromotionType;
import com.uade.elrincondelmazo.exception.InvalidPromotionException;
import com.uade.elrincondelmazo.exception.ResourceNotFoundException;
import com.uade.elrincondelmazo.repository.CollectionRepository;
import com.uade.elrincondelmazo.repository.ProductRepository;
import com.uade.elrincondelmazo.repository.PromotionRepository;
import com.uade.elrincondelmazo.service.PromotionService;

@Service
public class PromotionServiceImpl implements PromotionService {

        /*
         * Servicio encargado de la lógica de negocio relacionada a promociones.
         * - Crea, actualiza y activa/desactiva promociones (CRUD básico).
         * - Calcula y aplica promociones sobre un carrito de compras.
         *
         * Las funciones principales están organizadas en secciones:
         *  - Administración (RDM-46)
         *  - Aplicación de promociones (RDM-47)
         *  - Validaciones y helpers
         */

        private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

        @Autowired
        private PromotionRepository promotionRepository;

        @Autowired
        private CollectionRepository collectionRepository;

        @Autowired
        private ProductRepository productRepository;

        // Constante para cálculos porcentuales (divide por 100)

        // ==========================================
        // RDM-46 - ADMINISTRACION DE PROMOCIONES
        // ==========================================

        @Override
        public List<PromotionResponse> getAllPromotions() {

                // Recupera todas las promociones de la BD y las mapea a DTOs
                return promotionRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Override
        public PromotionResponse getPromotionById(Long id) {


                // Busca la promoción o lanza excepción si no existe
                Promotion promotion = findPromotionById(id);

                // Convierte la entidad a DTO de respuesta
                return toResponse(promotion);
        }

        @Override
        public PromotionResponse createPromotion(
                        CreatePromotionRequest request) {


                // Validaciones básicas antes de persistir
                validateDates(
                                request.getStartDate(),
                                request.getEndDate());
                validateRequiredFields(
                                request.getType(),
                                request.getMinimumQuantity(),
                                request.getMinimumAmount(),
                                request.getProductType(),
                                request.getPaymentMethod(),
                                request.getCollectionId(),
                                request.getProductId());

                // Construye la entidad Promotion a partir del request
                Promotion promotion = new Promotion();

                promotion.setName(request.getName());
                promotion.setDescription(request.getDescription());
                promotion.setType(request.getType());
                promotion.setDiscountPercentage(request.getDiscountPercentage());
                promotion.setMinimumQuantity(request.getMinimumQuantity());
                promotion.setMinimumAmount(request.getMinimumAmount());
                promotion.setProductType(request.getProductType());
                promotion.setPaymentMethod(request.getPaymentMethod());
                promotion.setStackable(request.getStackable());
                promotion.setStartDate(request.getStartDate());
                promotion.setEndDate(request.getEndDate());

                // Relaciona colección/producto si vienen en el request
                promotion.setCollection(getCollection(request.getCollectionId()));
                promotion.setProduct(getProduct(request.getProductId()));

                // Nueva promoción activada por defecto
                promotion.setActive(true);

                Promotion saved = promotionRepository.save(promotion);

                return toResponse(saved);
        }

        @Override
        public PromotionResponse updatePromotion(
                        Long id,
                        UpdatePromotionRequest request) {


                // Actualiza una promoción existente: valida y asigna campos
                Promotion promotion = findPromotionById(id);

                validateDates(request.getStartDate(), request.getEndDate());
                validateRequiredFields(
                                request.getType(),
                                request.getMinimumQuantity(),
                                request.getMinimumAmount(),
                                request.getProductType(),
                                request.getPaymentMethod(),
                                request.getCollectionId(),
                                request.getProductId());

                promotion.setName(request.getName());
                promotion.setDescription(request.getDescription());
                promotion.setType(request.getType());
                promotion.setDiscountPercentage(request.getDiscountPercentage());
                promotion.setMinimumQuantity(request.getMinimumQuantity());
                promotion.setMinimumAmount(request.getMinimumAmount());
                promotion.setProductType(request.getProductType());
                promotion.setPaymentMethod(request.getPaymentMethod());
                promotion.setStackable(request.getStackable());
                promotion.setStartDate(request.getStartDate());
                promotion.setEndDate(request.getEndDate());
                promotion.setCollection(getCollection(request.getCollectionId()));
                promotion.setProduct(getProduct(request.getProductId()));

                Promotion updated = promotionRepository.save(promotion);

                return toResponse(updated);
        }

        @Override
        public PromotionResponse activatePromotion(
                        Long id) {


                // Activa la promoción especificada
                Promotion promotion = findPromotionById(id);
                promotion.setActive(true);
                return toResponse(promotionRepository.save(promotion));
        }

        @Override
        public PromotionResponse deactivatePromotion(
                        Long id) {


                // Desactiva la promoción especificada
                Promotion promotion = findPromotionById(id);
                promotion.setActive(false);
                return toResponse(promotionRepository.save(promotion));
        }

        // ==========================================
        // RDM-47 - APLICACION DE PROMOCIONES
        // ==========================================

        @Override
        @Transactional(readOnly = true)
        public PromotionApplicationResponse applyPromotions(
                        List<CartItem> items,
                        PaymentMethod paymentMethod) {


                // Si el carrito está vacío no hay descuentos
                if (items == null || items.isEmpty()) {
                        return new PromotionApplicationResponse(BigDecimal.ZERO, List.of());
                }

                // Calcula subtotal del carrito y momento actual
                BigDecimal subtotal = calculateSubtotal(items);
                LocalDateTime now = LocalDateTime.now();

                // Carga todas las promociones activas desde la BD
                List<Promotion> promotions = promotionRepository.findByActiveTrue();

                List<AppliedPromotionResponse> applicablePromotions = new ArrayList<>();

                // Recorre las promociones y calcula el descuento aplicable
                for (Promotion promotion : promotions) {

                        // Verifica vigencia y estado
                        if (!isValidAt(promotion, now)) {
                                continue;
                        }

                        // Obtiene la base sobre la cual se calcula el descuento según tipo
                        BigDecimal discountBase = getDiscountBase(promotion, items, subtotal, paymentMethod);

                        // Ignora si la base de descuento es 0 o negativa
                        if (discountBase.compareTo(BigDecimal.ZERO) <= 0) {
                                continue;
                        }

                        // Calcula el monto del descuento usando el porcentaje de la promoción
                        BigDecimal discount = calculateDiscount(discountBase, promotion.getDiscountPercentage());

                        if (discount.compareTo(BigDecimal.ZERO) > 0) {
                                AppliedPromotionResponse applied = new AppliedPromotionResponse(
                                                promotion.getId(), promotion.getName(), discount);
                                applicablePromotions.add(applied);
                        }
                }

                // Resuelve reglas de combinabilidad (stackable) y devuelve resultado final
                return applyStackableRules(applicablePromotions, promotions, subtotal);
        }

        // ==========================================
        // VALIDACION DE VIGENCIA
        // ==========================================

        private boolean isValidAt(
                        Promotion promotion,
                        LocalDateTime now) {

                // Valida que la promoción esté activa y dentro del rango de fechas
                if (!promotion.isActive()) {
                        return false;
                }

                if (promotion.getStartDate() == null) {
                        return false;
                }

                if (promotion.getStartDate().isAfter(now)) {
                        return false;
                }

                // Si no tiene endDate se considera válida; si tiene, no puede ser anterior a 'now'
                return promotion.getEndDate() == null || !promotion.getEndDate().isBefore(now);
        }

        // ==========================================
        // CONDICIONES DE PROMOCION
        // ==========================================

        private BigDecimal getDiscountBase(
                        Promotion promotion,
                        List<CartItem> items,
                        BigDecimal subtotal,
                        PaymentMethod paymentMethod) {

                // Determina la "base" sobre la cual se calculará el descuento
                // según el tipo de promoción (cantidad, colección, producto, etc.)
                return switch (promotion.getType()) {
                        case CANTIDAD -> getQuantityDiscountBase(promotion, items, subtotal);
                        case COLECCION -> getCollectionDiscountBase(promotion, items);
                        case TIPO_PRODUCTO -> getProductTypeDiscountBase(promotion, items);
                        case METODO_PAGO -> getPaymentMethodDiscountBase(promotion, subtotal, paymentMethod);
                        case MONTO_MINIMO -> getMinimumAmountDiscountBase(promotion, subtotal);
                        case PRODUCTO -> getProductDiscountBase(promotion, items);
                };
        }

        private BigDecimal getQuantityDiscountBase(
                        Promotion promotion,
                        List<CartItem> items,
                        BigDecimal subtotal) {

                // Para promociones por cantidad: si la cantidad total del carrito
                // es menor al mínimo requerido, la base es 0; sino es el subtotal.
                if (promotion.getMinimumQuantity() == null) {
                        return BigDecimal.ZERO;
                }

                int totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();

                if (totalQuantity < promotion.getMinimumQuantity()) {
                        return BigDecimal.ZERO;
                }

                return subtotal;
        }

        private BigDecimal getCollectionDiscountBase(
                        Promotion promotion,
                        List<CartItem> items) {

                // Suma los subtotales de los items que pertenecen a la colección
                if (promotion.getCollection() == null) {
                        return BigDecimal.ZERO;
                }

                return items.stream()
                                .filter(item -> item.getProduct().getCollection() != null
                                                && Objects.equals(item.getProduct().getCollection().getId(),
                                                                promotion.getCollection().getId()))
                                .map(this::calculateItemSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal getProductTypeDiscountBase(
                        Promotion promotion,
                        List<CartItem> items) {

                // Suma subtotales de items que coinciden con el tipo de producto
                if (promotion.getProductType() == null) {
                        return BigDecimal.ZERO;
                }

                return items.stream()
                                .filter(item -> item.getProduct().getType() == promotion.getProductType())
                                .map(this::calculateItemSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal getPaymentMethodDiscountBase(
                        Promotion promotion,
                        BigDecimal subtotal,
                        PaymentMethod paymentMethod) {

                // Si la promoción exige un método de pago, compara con el usado
                if (promotion.getPaymentMethod() == null || paymentMethod == null) {
                        return BigDecimal.ZERO;
                }

                if (promotion.getPaymentMethod() != paymentMethod) {
                        return BigDecimal.ZERO;
                }

                return subtotal;
        }

        private BigDecimal getProductDiscountBase(
                        Promotion promotion,
                        List<CartItem> items) {

                // Suma subtotales de items que coinciden con el producto objetivo
                if (promotion.getProduct() == null) {
                        return BigDecimal.ZERO;
                }

                return items.stream()
                                .filter(item -> Objects.equals(item.getProduct().getId(),
                                                promotion.getProduct().getId()))
                                .map(this::calculateItemSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal getMinimumAmountDiscountBase(
                        Promotion promotion,
                        BigDecimal subtotal) {

                // Si el subtotal cumple el mínimo requerido, la base es el subtotal
                if (promotion.getMinimumAmount() == null) {
                        return BigDecimal.ZERO;
                }

                if (subtotal.compareTo(promotion.getMinimumAmount()) < 0) {
                        return BigDecimal.ZERO;
                }

                return subtotal;
        }

        // ==========================================
        // CALCULOS
        // ==========================================

        private BigDecimal calculateSubtotal(
                        List<CartItem> items) {

                // Suma de subtotales de cada item (price * quantity)
                return items.stream().map(this::calculateItemSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal calculateItemSubtotal(
                        CartItem item) {

                Product product = item.getProduct();

                // Precio unitario * cantidad
                return product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        }

        private BigDecimal calculateDiscount(
                        BigDecimal base,
                        BigDecimal percentage) {

                // Calcula el descuento: base * (percentage / 100)
                if (percentage == null) {
                        return BigDecimal.ZERO;
                }

                return base.multiply(percentage).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        }

        // ==========================================
        // STACKABLE
        // ==========================================

        private PromotionApplicationResponse applyStackableRules(
                List<AppliedPromotionResponse> applicablePromotions,
                List<Promotion> promotions,BigDecimal subtotal) {


                // Si no hay promociones aplicables retorna 0
                if (applicablePromotions.isEmpty()) {
                        return new PromotionApplicationResponse(BigDecimal.ZERO, List.of());
                }

                // Separa promociones que permiten combinarse (stackable) y las que no
                List<AppliedPromotionResponse> stackablePromotions = applicablePromotions.stream()
                                .filter(applied -> isStackable(applied.getPromotionId(), promotions))
                                .toList();

                List<AppliedPromotionResponse> nonStackablePromotions = applicablePromotions.stream()
                                .filter(applied -> !isStackable(applied.getPromotionId(), promotions))
                                .toList();

                // Si no hay promociones no-stackable, aplicamos todas las stackables
                // hasta agotar el subtotal (no aplicar más descuento que el subtotal)
                if (nonStackablePromotions.isEmpty()) {

                         List<AppliedPromotionResponse> appliedWithinSubtotal = new ArrayList<>();

                        BigDecimal remainingDiscount = subtotal;

                        for (AppliedPromotionResponse applied : stackablePromotions) {

                                if (remainingDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                                        break;
                                }

                                // No permitir que el descuento aplicado supere lo que queda del subtotal
                                BigDecimal discountToApply = applied.getDiscountApplied().min(remainingDiscount);

                                appliedWithinSubtotal.add(new AppliedPromotionResponse(
                                        applied.getPromotionId(), applied.getPromotionName(), discountToApply));

                                remainingDiscount = remainingDiscount.subtract(discountToApply);
                        }

                        BigDecimal totalDiscount = subtotal.subtract(remainingDiscount);

                        return new PromotionApplicationResponse(totalDiscount, appliedWithinSubtotal);
                }

                // Si hay al menos una no-stackable, selecciona la mejor (mayor descuento)
                AppliedPromotionResponse bestNonStackable = nonStackablePromotions.stream()
                                .max(Comparator.comparing(AppliedPromotionResponse::getDiscountApplied))
                                .orElseThrow();

                return new PromotionApplicationResponse(bestNonStackable.getDiscountApplied(), List.of(bestNonStackable));
        }

        private boolean isStackable(
                        Long promotionId,
                        List<Promotion> promotions) {

                // Busca la promoción por id y devuelve su flag 'stackable'
                return promotions.stream()
                                .filter(promotion -> Objects.equals(promotion.getId(), promotionId))
                                .findFirst()
                                .map(Promotion::isStackable)
                                .orElse(false);
        }

        // ==========================================
        // HELPERS RDM-46
        // ==========================================

        private Promotion findPromotionById(
                        Long id) {

                // Helper para recuperar una promoción o lanzar excepción si no existe
                return promotionRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Promocion no encontrada con id: " + id));
        }

        private Collection getCollection(
                        Long collectionId) {

                // Recupera colección por id o devuelve null si id nulo
                if (collectionId == null) {
                        return null;
                }

                return collectionRepository.findById(collectionId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Coleccion no encontrada con id: " + collectionId));
        }

        private Product getProduct(
                        Long productId) {

                // Recupera producto por id o devuelve null si id nulo
                if (productId == null) {
                        return null;
                }

                return productRepository.findById(productId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Producto no encontrado con id: " + productId));
        }

        private void validateDates(
                        LocalDateTime startDate,
                        LocalDateTime endDate) {

                // Valida que la fecha de fin no sea anterior a la de inicio
                if (endDate != null && endDate.isBefore(startDate)) {
                        throw new InvalidPromotionException(
                                        "La fecha de fin no puede ser anterior a la fecha de inicio");
                }
        }

        private void validateRequiredFields(
                 PromotionType type,
                Integer minimumQuantity,
                BigDecimal minimumAmount,
                ProductType productType,
                PaymentMethod paymentMethod,
                Long collectionId,
                Long productId) {

         // Verifica que los campos requeridos por cada tipo de promoción estén presentes
         switch (type) {

                case CANTIDAD -> {
                        if (minimumQuantity == null) {
                                throw new InvalidPromotionException("La promocion CANTIDAD requiere minimumQuantity");
                        }
                }

                case COLECCION -> {
                        if (collectionId == null) {
                                throw new InvalidPromotionException("La promocion COLECCION requiere collectionId");
                        }
                }

                case TIPO_PRODUCTO -> {
                        if (productType == null) {
                                throw new InvalidPromotionException("La promocion TIPO_PRODUCTO requiere productType");
                        }
                }

                case METODO_PAGO -> {
                        if (paymentMethod == null) {
                                throw new InvalidPromotionException("La promocion METODO_PAGO requiere paymentMethod");
                        }
                }

                case MONTO_MINIMO -> {
                        if (minimumAmount == null) {
                                throw new InvalidPromotionException("La promocion MONTO_MINIMO requiere minimumAmount");
                        }
                }

                case PRODUCTO -> {
                        if (productId == null) {
                                throw new InvalidPromotionException("La promocion PRODUCTO requiere productId");
                        }
                }
        }
    }

        private PromotionResponse toResponse(
                        Promotion promotion) {

                PromotionResponse response = new PromotionResponse();

                response.setId(
                                promotion.getId());

                response.setName(
                                promotion.getName());

                response.setDescription(
                                promotion.getDescription());

                response.setType(
                                promotion.getType());

                response.setDiscountPercentage(
                                promotion.getDiscountPercentage());

                response.setMinimumQuantity(
                                promotion.getMinimumQuantity());

                response.setMinimumAmount(
                                promotion.getMinimumAmount());

                response.setProductType(
                                promotion.getProductType());

                response.setPaymentMethod(
                                promotion.getPaymentMethod());

                if (promotion.getCollection() != null) {

                        response.setCollectionId(
                                        promotion.getCollection()
                                                        .getId());

                        response.setCollectionName(
                                        promotion.getCollection()
                                                        .getName());
                }

                if (promotion.getProduct() != null) {

                        response.setProductId(
                                        promotion.getProduct().getId());

                        response.setProductName(
                                        promotion.getProduct().getName());
                }

                response.setStackable(
                                promotion.isStackable());

                response.setStartDate(
                                promotion.getStartDate());

                response.setEndDate(
                                promotion.getEndDate());

                response.setActive(
                                promotion.isActive());

                return response;
        }
}
