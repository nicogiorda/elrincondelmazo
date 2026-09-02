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
import com.uade.elrincondelmazo.exception.InvalidPromotionException;
import com.uade.elrincondelmazo.exception.ResourceNotFoundException;
import com.uade.elrincondelmazo.repository.CollectionRepository;
import com.uade.elrincondelmazo.repository.PromotionRepository;
import com.uade.elrincondelmazo.service.PromotionService;

@Service
public class PromotionServiceImpl implements PromotionService {

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    // ==========================================
    // RDM-46 - ADMINISTRACION DE PROMOCIONES
    // ==========================================

    @Override
    public List<PromotionResponse> getAllPromotions() {

        return promotionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PromotionResponse getPromotionById(Long id) {

        Promotion promotion = findPromotionById(id);

        return toResponse(promotion);
    }

    @Override
    public PromotionResponse createPromotion(
            CreatePromotionRequest request) {

        validateDates(
                request.getStartDate(),
                request.getEndDate());

        Promotion promotion = new Promotion();

        promotion.setName(
                request.getName());

        promotion.setDescription(
                request.getDescription());

        promotion.setType(
                request.getType());

        promotion.setDiscountPercentage(
                request.getDiscountPercentage());

        promotion.setMinimumQuantity(
                request.getMinimumQuantity());

        promotion.setMinimumAmount(
                request.getMinimumAmount());

        promotion.setProductType(
                request.getProductType());

        promotion.setPaymentMethod(
                request.getPaymentMethod());

        promotion.setStackable(
                request.getStackable());

        promotion.setStartDate(
                request.getStartDate());

        promotion.setEndDate(
                request.getEndDate());

        promotion.setCollection(
                getCollection(
                        request.getCollectionId()));

        promotion.setActive(true);

        Promotion saved =
                promotionRepository.save(promotion);

        return toResponse(saved);
    }

    @Override
    public PromotionResponse updatePromotion(
            Long id,
            UpdatePromotionRequest request) {

        Promotion promotion =
                findPromotionById(id);

        validateDates(
                request.getStartDate(),
                request.getEndDate());

        promotion.setName(
                request.getName());

        promotion.setDescription(
                request.getDescription());

        promotion.setType(
                request.getType());

        promotion.setDiscountPercentage(
                request.getDiscountPercentage());

        promotion.setMinimumQuantity(
                request.getMinimumQuantity());

        promotion.setMinimumAmount(
                request.getMinimumAmount());

        promotion.setProductType(
                request.getProductType());

        promotion.setPaymentMethod(
                request.getPaymentMethod());

        promotion.setStackable(
                request.getStackable());

        promotion.setStartDate(
                request.getStartDate());

        promotion.setEndDate(
                request.getEndDate());

        promotion.setCollection(
                getCollection(
                        request.getCollectionId()));

        Promotion updated =
                promotionRepository.save(promotion);

        return toResponse(updated);
    }

    @Override
    public PromotionResponse activatePromotion(
            Long id) {

        Promotion promotion =
                findPromotionById(id);

        promotion.setActive(true);

        return toResponse(
                promotionRepository.save(promotion));
    }

    @Override
    public PromotionResponse deactivatePromotion(
            Long id) {

        Promotion promotion =
                findPromotionById(id);

        promotion.setActive(false);

        return toResponse(
                promotionRepository.save(promotion));
    }

    // ==========================================
    // RDM-47 - APLICACION DE PROMOCIONES
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public PromotionApplicationResponse applyPromotions(
            List<CartItem> items,
            PaymentMethod paymentMethod) {

        if (items == null || items.isEmpty()) {

            return new PromotionApplicationResponse(
                    BigDecimal.ZERO,
                    List.of());
        }

        BigDecimal subtotal =
                calculateSubtotal(items);

        LocalDateTime now =
                LocalDateTime.now();

        List<Promotion> promotions =
                promotionRepository.findByActiveTrue();

        List<AppliedPromotionResponse> applicablePromotions =
                new ArrayList<>();

        for (Promotion promotion : promotions) {

            if (!isValidAt(
                    promotion,
                    now)) {

                continue;
            }

            BigDecimal discountBase =
                    getDiscountBase(
                            promotion,
                            items,
                            subtotal,
                            paymentMethod);

            if (discountBase.compareTo(
                    BigDecimal.ZERO) <= 0) {

                continue;
            }

            BigDecimal discount =
                    calculateDiscount(
                            discountBase,
                            promotion.getDiscountPercentage());

            if (discount.compareTo(
                    BigDecimal.ZERO) > 0) {

                AppliedPromotionResponse applied =
                        new AppliedPromotionResponse(
                                promotion.getId(),
                                promotion.getName(),
                                discount);

                applicablePromotions.add(applied);
            }
        }

        return applyStackableRules(
                applicablePromotions,
                promotions);
    }

    // ==========================================
    // VALIDACION DE VIGENCIA
    // ==========================================

    private boolean isValidAt(
            Promotion promotion,
            LocalDateTime now) {

        if (!promotion.isActive()) {
            return false;
        }

        if (promotion.getStartDate() == null) {
            return false;
        }

        if (promotion.getStartDate().isAfter(now)) {
            return false;
        }

        return promotion.getEndDate() == null
                || !promotion.getEndDate().isBefore(now);
    }

    // ==========================================
    // CONDICIONES DE PROMOCION
    // ==========================================

    private BigDecimal getDiscountBase(
            Promotion promotion,
            List<CartItem> items,
            BigDecimal subtotal,
            PaymentMethod paymentMethod) {

        return switch (promotion.getType()) {

            case CANTIDAD ->
                    getQuantityDiscountBase(
                            promotion,
                            items,
                            subtotal);

            case COLECCION ->
                    getCollectionDiscountBase(
                            promotion,
                            items);

            case TIPO_PRODUCTO ->
                    getProductTypeDiscountBase(
                            promotion,
                            items);

            case METODO_PAGO ->
                    getPaymentMethodDiscountBase(
                            promotion,
                            subtotal,
                            paymentMethod);

            case MONTO_MINIMO ->
                    getMinimumAmountDiscountBase(
                            promotion,
                            subtotal);
        };
    }

    private BigDecimal getQuantityDiscountBase(
            Promotion promotion,
            List<CartItem> items,
            BigDecimal subtotal) {

        if (promotion.getMinimumQuantity() == null) {
            return BigDecimal.ZERO;
        }

        int totalQuantity =
                items.stream()
                        .mapToInt(
                                CartItem::getQuantity)
                        .sum();

        if (totalQuantity
                < promotion.getMinimumQuantity()) {

            return BigDecimal.ZERO;
        }

        return subtotal;
    }

    private BigDecimal getCollectionDiscountBase(
            Promotion promotion,
            List<CartItem> items) {

        if (promotion.getCollection() == null) {
            return BigDecimal.ZERO;
        }

        return items.stream()
                .filter(item ->
                        item.getProduct()
                                .getCollection() != null
                                && Objects.equals(
                                        item.getProduct()
                                                .getCollection()
                                                .getId(),
                                        promotion
                                                .getCollection()
                                                .getId()))
                .map(this::calculateItemSubtotal)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add);
    }

    private BigDecimal getProductTypeDiscountBase(
            Promotion promotion,
            List<CartItem> items) {

        if (promotion.getProductType() == null) {
            return BigDecimal.ZERO;
        }

        return items.stream()
                .filter(item ->
                        item.getProduct()
                                .getType()
                                == promotion
                                        .getProductType())
                .map(this::calculateItemSubtotal)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add);
    }

    private BigDecimal getPaymentMethodDiscountBase(
            Promotion promotion,
            BigDecimal subtotal,
            PaymentMethod paymentMethod) {

        if (promotion.getPaymentMethod() == null
                || paymentMethod == null) {

            return BigDecimal.ZERO;
        }

        if (promotion.getPaymentMethod()
                != paymentMethod) {

            return BigDecimal.ZERO;
        }

        return subtotal;
    }

    private BigDecimal getMinimumAmountDiscountBase(
            Promotion promotion,
            BigDecimal subtotal) {

        if (promotion.getMinimumAmount() == null) {
            return BigDecimal.ZERO;
        }

        if (subtotal.compareTo(
                promotion.getMinimumAmount()) < 0) {

            return BigDecimal.ZERO;
        }

        return subtotal;
    }

    // ==========================================
    // CALCULOS
    // ==========================================

    private BigDecimal calculateSubtotal(
            List<CartItem> items) {

        return items.stream()
                .map(this::calculateItemSubtotal)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add);
    }

    private BigDecimal calculateItemSubtotal(
            CartItem item) {

        Product product =
                item.getProduct();

        return product.getPrice()
                .multiply(
                        BigDecimal.valueOf(
                                item.getQuantity()));
    }

    private BigDecimal calculateDiscount(
            BigDecimal base,
            BigDecimal percentage) {

        if (percentage == null) {
            return BigDecimal.ZERO;
        }

        return base
                .multiply(percentage)
                .divide(
                        ONE_HUNDRED,
                        2,
                        RoundingMode.HALF_UP);
    }

    // ==========================================
    // STACKABLE
    // ==========================================

    private PromotionApplicationResponse applyStackableRules(
            List<AppliedPromotionResponse> applicablePromotions,
            List<Promotion> promotions) {

        if (applicablePromotions.isEmpty()) {

            return new PromotionApplicationResponse(
                    BigDecimal.ZERO,
                    List.of());
        }

        List<AppliedPromotionResponse> stackablePromotions =
                applicablePromotions.stream()
                        .filter(applied ->
                                isStackable(
                                        applied.getPromotionId(),
                                        promotions))
                        .toList();

        List<AppliedPromotionResponse> nonStackablePromotions =
                applicablePromotions.stream()
                        .filter(applied ->
                                !isStackable(
                                        applied.getPromotionId(),
                                        promotions))
                        .toList();

        if (nonStackablePromotions.isEmpty()) {

            BigDecimal totalDiscount =
                    stackablePromotions.stream()
                            .map(
                                    AppliedPromotionResponse::
                                            getDiscountApplied)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add);

            return new PromotionApplicationResponse(
                    totalDiscount,
                    stackablePromotions);
        }

        AppliedPromotionResponse bestNonStackable =
                nonStackablePromotions.stream()
                        .max(
                                Comparator.comparing(
                                        AppliedPromotionResponse::
                                                getDiscountApplied))
                        .orElseThrow();

        return new PromotionApplicationResponse(
                bestNonStackable.getDiscountApplied(),
                List.of(bestNonStackable));
    }

    private boolean isStackable(
            Long promotionId,
            List<Promotion> promotions) {

        return promotions.stream()
                .filter(promotion ->
                        Objects.equals(
                                promotion.getId(),
                                promotionId))
                .findFirst()
                .map(Promotion::isStackable)
                .orElse(false);
    }

    // ==========================================
    // HELPERS RDM-46
    // ==========================================

    private Promotion findPromotionById(
            Long id) {

        return promotionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Promocion no encontrada con id: "
                                        + id));
    }

    private Collection getCollection(
            Long collectionId) {

        if (collectionId == null) {
            return null;
        }

        return collectionRepository
                .findById(collectionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Coleccion no encontrada con id: "
                                        + collectionId));
    }

    private void validateDates(
            LocalDateTime startDate,
            LocalDateTime endDate) {

        if (endDate != null
                && endDate.isBefore(startDate)) {

            throw new InvalidPromotionException(
                    "La fecha de fin no puede ser anterior a la fecha de inicio");
        }
    }

    private PromotionResponse toResponse(
            Promotion promotion) {

        PromotionResponse response =
                new PromotionResponse();

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

