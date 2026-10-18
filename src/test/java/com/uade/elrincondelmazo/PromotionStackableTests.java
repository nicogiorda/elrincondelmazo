package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.entity.CartItem;
import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.entity.Promotion;
import com.uade.elrincondelmazo.entity.dto.AppliedPromotionResponse;
import com.uade.elrincondelmazo.entity.dto.PromotionApplicationResponse;
import com.uade.elrincondelmazo.enums.PaymentMethod;
import com.uade.elrincondelmazo.enums.ProductType;
import com.uade.elrincondelmazo.enums.PromotionType;
import com.uade.elrincondelmazo.repository.PromotionRepository;
import com.uade.elrincondelmazo.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionStackableTests {
    @Mock PromotionRepository promotions;
    @InjectMocks PromotionServiceImpl service;

    @ParameterizedTest
    @CsvSource({"30,30,10,60,true", "5,5,25,25,false", "80,80,70,100,true", "15,15,30,30,false", "80,80,100,100,false"})
    void choosesGreatestSavingAndNonStackableOnTie(String a, String b, String c, String expected, boolean stacksWin) {
        Promotion first = promotion(1, a, true);
        Promotion second = promotion(2, b, true);
        Promotion exclusive = promotion(3, c, false);
        when(promotions.findByActiveTrue()).thenReturn(List.of(first, second, exclusive));
        PromotionApplicationResponse result = apply(List.of(item(1, ProductType.CARTA, 1, "100.00", 10)));
        assertThat(result.getTotalDiscount()).isEqualByComparingTo(expected);
        assertThat(result.getAppliedPromotions()).extracting(AppliedPromotionResponse::getPromotionId)
                .containsExactlyElementsOf(stacksWin ? List.of(1L, 2L) : List.of(3L));
        if (stacksWin) {
            assertThat(result.getAppliedPromotions().getFirst().getDiscountApplied()).isEqualByComparingTo(a);
            assertThat(result.getAppliedPromotions().get(1).getDiscountApplied())
                    .isEqualByComparingTo(new BigDecimal(expected).subtract(new BigDecimal(a)));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void tiedNonStackablePromotionsUseLowestIdIndependentOfRepositoryOrder(boolean reverse) {
        Promotion higherId = promotion(9, "25", false);
        Promotion lowerId = promotion(3, "25", false);
        Promotion smallerDiscount = promotion(1, "10", false);
        when(promotions.findByActiveTrue()).thenReturn(reverse
                ? List.of(lowerId, smallerDiscount, higherId) : List.of(higherId, smallerDiscount, lowerId));
        PromotionApplicationResponse result = apply(List.of(item(1, ProductType.CARTA, 1, "100.00", 10)));
        assertThat(result.getTotalDiscount()).isEqualByComparingTo("25.00");
        assertThat(result.getAppliedPromotions()).extracting(AppliedPromotionResponse::getPromotionId).containsExactly(3L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void onePromotionWorksWithEitherStackableFlag(boolean stackable) {
        when(promotions.findByActiveTrue()).thenReturn(List.of(promotion(1, "17.5", stackable)));
        PromotionApplicationResponse result = apply(List.of(item(1, ProductType.CARTA, 1, "100.00", 10)));
        assertThat(result.getTotalDiscount()).isEqualByComparingTo("17.50");
        assertThat(result.getAppliedPromotions()).hasSize(1);
    }

    @Test
    void noApplicablePromotionProducesEmptyBreakdown() {
        Promotion belowThreshold = promotion(1, "50", false);
        belowThreshold.setMinimumAmount(new BigDecimal("200"));
        when(promotions.findByActiveTrue()).thenReturn(List.of(belowThreshold));
        PromotionApplicationResponse result = apply(List.of(item(1, ProductType.CARTA, 1, "100.00", 10)));
        assertThat(result.getTotalDiscount()).isZero();
        assertThat(result.getAppliedPromotions()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "scheduled", "inactive", "missingStart"})
    void invalidValidityDoesNotDisplaceApplicableStackables(String condition) {
        Promotion valid = promotion(1, "30", true);
        Promotion invalid = promotion(2, "100", false);
        switch (condition) {
            case "expired" -> invalid.setEndDate(LocalDateTime.now().minusDays(1));
            case "scheduled" -> invalid.setStartDate(LocalDateTime.now().plusDays(1));
            case "inactive" -> invalid.setActive(false);
            case "missingStart" -> invalid.setStartDate(null);
            default -> throw new AssertionError(condition);
        }
        when(promotions.findByActiveTrue()).thenReturn(List.of(invalid, valid));
        PromotionApplicationResponse result = apply(List.of(item(1, ProductType.CARTA, 1, "100.00", 10)));
        assertThat(result.getTotalDiscount()).isEqualByComparingTo("30.00");
        assertThat(result.getAppliedPromotions()).extracting(AppliedPromotionResponse::getPromotionId).containsExactly(1L);
    }

    @Test
    void noStoredPromotionsProducesNoDiscount() {
        when(promotions.findByActiveTrue()).thenReturn(List.of());
        assertThat(apply(List.of(item(1, ProductType.CARTA, 1, "100.00", 10))).getAppliedPromotions()).isEmpty();
    }

    @Test
    void emptyCartProducesNoDiscount() {
        PromotionApplicationResponse result = apply(List.of());
        assertThat(result.getAppliedPromotions()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(PromotionType.class)
    void sixTypesKeepTheirIndividualBasesWhenComparedWithNonStackable(PromotionType type) {
        CartItem envelopes = item(1, ProductType.SOBRE, 3, "10.00", 10);
        CartItem card = item(2, ProductType.CARTA, 1, "70.00", 20);
        Promotion specific = promotion(1, "30", true);
        configureType(specific, type, envelopes);
        Promotion payment = promotion(2, "5", true);
        payment.setType(PromotionType.METODO_PAGO);
        payment.setPaymentMethod(PaymentMethod.MERCADO_PAGO);
        Promotion exclusive = promotion(3, "10", false);
        when(promotions.findByActiveTrue()).thenReturn(List.of(specific, payment, exclusive));
        PromotionApplicationResponse result = apply(List.of(envelopes, card));
        boolean wholeCart = type == PromotionType.MONTO_MINIMO || type == PromotionType.METODO_PAGO;
        assertThat(result.getTotalDiscount()).isEqualByComparingTo(wholeCart ? "35.00" : "14.00");
        assertThat(result.getAppliedPromotions()).extracting(AppliedPromotionResponse::getPromotionId).containsExactly(1L, 2L);
        assertThat(result.getAppliedPromotions().getFirst().getDiscountApplied())
                .isEqualByComparingTo(wholeCart ? "30.00" : "9.00");
    }

    @Test
    void allSixTypesCanStackOnTheirDifferentBasesWithoutSequentialPercentages() {
        CartItem envelopes = item(1, ProductType.SOBRE, 3, "10.00", 10);
        CartItem card = item(2, ProductType.CARTA, 1, "70.00", 20);
        List<Promotion> candidates = new java.util.ArrayList<>();
        long id = 1;
        for (PromotionType type : PromotionType.values()) {
            Promotion promotion = promotion(id++, "10", true);
            configureType(promotion, type, envelopes);
            candidates.add(promotion);
        }
        candidates.add(promotion(99, "10", false));
        when(promotions.findByActiveTrue()).thenReturn(candidates);
        PromotionApplicationResponse result = apply(List.of(envelopes, card));
        assertThat(result.getTotalDiscount()).isEqualByComparingTo("32.00");
        assertThat(result.getAppliedPromotions()).hasSize(6);
        assertThat(result.getAppliedPromotions()).extracting(AppliedPromotionResponse::getPromotionId).doesNotContain(99L);
    }

    @Test
    void individualAmountsUseHalfUpBeforeSumming() {
        when(promotions.findByActiveTrue()).thenReturn(List.of(promotion(1, "5", true), promotion(2, "5", true),
                promotion(3, "5", false)));
        PromotionApplicationResponse result = apply(List.of(item(1, ProductType.CARTA, 1, "0.10", 10)));
        assertThat(result.getTotalDiscount()).isEqualByComparingTo("0.02");
        assertThat(result.getAppliedPromotions()).extracting(AppliedPromotionResponse::getDiscountApplied)
                .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("0.01"));
    }

    @Test
    void centRoundingAndSubtotalCapDoNotIncludeUnusedPromotions() {
        when(promotions.findByActiveTrue()).thenReturn(List.of(promotion(1, "80", true), promotion(2, "80", true),
                promotion(3, "80", true)));
        PromotionApplicationResponse result = apply(List.of(item(1, ProductType.CARTA, 1, "0.05", 10)));
        assertThat(result.getTotalDiscount()).isEqualByComparingTo("0.05");
        assertThat(result.getAppliedPromotions()).extracting(AppliedPromotionResponse::getDiscountApplied)
                .containsExactly(new BigDecimal("0.04"), new BigDecimal("0.01"));
        assertThat(result.getAppliedPromotions()).extracting(AppliedPromotionResponse::getPromotionId).containsExactly(1L, 2L);
    }

    @Test
    void defensiveNonStackableCapNeverExceedsSubtotal() {
        // Legacy DB data is not necessarily subject to today's request validation.
        when(promotions.findByActiveTrue()).thenReturn(List.of(promotion(1, "150", false)));
        PromotionApplicationResponse result = apply(List.of(item(1, ProductType.CARTA, 1, "100.00", 10)));
        assertThat(result.getTotalDiscount()).isEqualByComparingTo("100.00");
        assertThat(result.getAppliedPromotions().getFirst().getDiscountApplied()).isEqualByComparingTo("100.00");
    }

    private PromotionApplicationResponse apply(List<CartItem> items) {
        PromotionApplicationResponse result = service.applyPromotions(items, PaymentMethod.MERCADO_PAGO);
        BigDecimal subtotal = items.stream().map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal breakdown = result.getAppliedPromotions().stream().map(AppliedPromotionResponse::getDiscountApplied)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(breakdown).isEqualByComparingTo(result.getTotalDiscount());
        assertThat(result.getTotalDiscount()).isBetween(BigDecimal.ZERO, subtotal);
        assertThat(subtotal.subtract(result.getTotalDiscount())).isNotNegative();
        result.getAppliedPromotions().forEach(p -> {
            assertThat(p.getDiscountApplied()).isPositive();
            assertThat(p.getDiscountApplied().scale()).isEqualTo(2);
        });
        return result;
    }

    private void configureType(Promotion p, PromotionType type, CartItem envelopes) {
        p.setType(type);
        p.setProductType(ProductType.SOBRE);
        p.setMinimumQuantity(3);
        p.setCollection(envelopes.getProduct().getCollection());
        p.setProduct(envelopes.getProduct());
        p.setMinimumAmount(new BigDecimal("100"));
        p.setPaymentMethod(PaymentMethod.MERCADO_PAGO);
    }

    private Promotion promotion(long id, String percentage, boolean stackable) {
        Promotion p = new Promotion();
        p.setId(id);
        p.setName("QA unit " + id);
        p.setType(PromotionType.MONTO_MINIMO);
        p.setMinimumAmount(new BigDecimal("0.01"));
        p.setDiscountPercentage(new BigDecimal(percentage));
        p.setStackable(stackable);
        p.setActive(true);
        p.setStartDate(LocalDateTime.now().minusDays(2));
        return p;
    }

    private CartItem item(long id, ProductType type, int quantity, String price, long collectionId) {
        Collection c = new Collection();
        c.setId(collectionId);
        Product p = new Product();
        p.setId(id);
        p.setType(type);
        p.setPrice(new BigDecimal(price));
        p.setCollection(c);
        CartItem item = new CartItem();
        item.setProduct(p);
        item.setQuantity(quantity);
        return item;
    }
}
