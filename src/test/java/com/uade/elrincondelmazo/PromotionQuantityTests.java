package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.entity.CartItem;
import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.entity.Promotion;
import com.uade.elrincondelmazo.entity.dto.PromotionApplicationResponse;
import com.uade.elrincondelmazo.enums.PaymentMethod;
import com.uade.elrincondelmazo.enums.ProductType;
import com.uade.elrincondelmazo.enums.PromotionType;
import com.uade.elrincondelmazo.repository.PromotionRepository;
import com.uade.elrincondelmazo.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionQuantityTests {

    @Mock PromotionRepository promotions;
    @InjectMocks PromotionServiceImpl service;

    Promotion quantity;

    @BeforeEach
    void setUp() {
        quantity = promotion(1L, PromotionType.CANTIDAD, "10");
        quantity.setMinimumQuantity(3);
    }

    @ParameterizedTest
    @EnumSource(ProductType.class)
    void threeUnitsOfRequiredTypeQualify(ProductType type) {
        quantity.setProductType(type);
        assertDiscount(List.of(item(type, 3, "100")), "30.00");
    }

    @ParameterizedTest
    @EnumSource(ProductType.class)
    void otherTypesDoNotCompleteMinimumQuantity(ProductType type) {
        quantity.setProductType(type);
        assertDiscount(List.of(item(type, 2, "100"), item(otherType(type), 1, "500")), "0");
    }

    @ParameterizedTest
    @EnumSource(ProductType.class)
    void onlyMatchingSubtotalIsDiscountedAcrossCartLines(ProductType type) {
        quantity.setProductType(type);
        assertDiscount(List.of(item(type, 2, "100"), item(type, 1, "200"),
                item(otherType(type), 1, "500")), "40.00");
    }

    @Test
    void nullTypeCountsMixedUnitsAndDiscountsWholeSubtotal() {
        assertDiscount(List.of(item(ProductType.SOBRE, 2, "100"),
                item(ProductType.CARTA, 1, "500")), "70.00");
    }

    @Test
    void nullTypeStillRequiresMinimumQuantity() {
        assertDiscount(List.of(item(ProductType.SOBRE, 1, "100"),
                item(ProductType.CARTA, 1, "500")), "0");
    }

    @Test
    void missingMinimumQuantityDoesNotApply() {
        quantity.setMinimumQuantity(null);
        assertDiscount(List.of(item(ProductType.SOBRE, 3, "100")), "0");
    }

    @ParameterizedTest
    @EnumSource(value = PromotionType.class, names = "CANTIDAD", mode = EnumSource.Mode.EXCLUDE)
    void otherFivePromotionTypesKeepTheirDiscountBase(PromotionType type) {
        CartItem envelopes = item(ProductType.SOBRE, 3, "100");
        CartItem card = item(ProductType.CARTA, 1, "500");
        Collection collection = new Collection();
        collection.setId(10L);
        envelopes.getProduct().setCollection(collection);
        envelopes.getProduct().setId(20L);
        card.getProduct().setId(21L);
        Promotion promotion = promotion(2L, type, "10");
        promotion.setCollection(collection);
        promotion.setProductType(ProductType.SOBRE);
        promotion.setProduct(envelopes.getProduct());
        promotion.setPaymentMethod(PaymentMethod.MERCADO_PAGO);
        promotion.setMinimumAmount(new BigDecimal("800"));
        when(promotions.findByActiveTrue()).thenReturn(List.of(promotion));

        PromotionApplicationResponse result = service.applyPromotions(
                List.of(envelopes, card), PaymentMethod.MERCADO_PAGO);

        boolean wholeCart = type == PromotionType.METODO_PAGO || type == PromotionType.MONTO_MINIMO;
        assertThat(result.getTotalDiscount()).isEqualByComparingTo(wholeCart ? "80.00" : "30.00");
        assertThat(result.getAppliedPromotions()).hasSize(1);
        assertThat(result.getAppliedPromotions().getFirst().getPromotionId()).isEqualTo(2L);
    }

    @Test
    void matchingQuantityDiscountStacksAndRemainsCappedAtSubtotal() {
        quantity.setProductType(ProductType.SOBRE);
        quantity.setDiscountPercentage(new BigDecimal("100"));
        Promotion payment = promotion(2L, PromotionType.METODO_PAGO, "100");
        payment.setPaymentMethod(PaymentMethod.MERCADO_PAGO);
        when(promotions.findByActiveTrue()).thenReturn(List.of(quantity, payment));

        PromotionApplicationResponse result = service.applyPromotions(
                List.of(item(ProductType.SOBRE, 3, "100"), item(ProductType.CARTA, 1, "500")),
                PaymentMethod.MERCADO_PAGO);

        assertThat(result.getTotalDiscount()).isEqualByComparingTo("800.00");
        assertThat(result.getAppliedPromotions()).hasSize(2);
        assertThat(result.getAppliedPromotions().getFirst().getDiscountApplied()).isEqualByComparingTo("300.00");
        assertThat(result.getAppliedPromotions().get(1).getDiscountApplied()).isEqualByComparingTo("500.00");
    }

    @Test
    void nonStackableQuantityDiscountKeepsExistingSelectionRules() {
        quantity.setProductType(ProductType.SOBRE);
        quantity.setDiscountPercentage(new BigDecimal("50"));
        quantity.setStackable(false);
        Promotion payment = promotion(2L, PromotionType.METODO_PAGO, "10");
        payment.setPaymentMethod(PaymentMethod.MERCADO_PAGO);
        when(promotions.findByActiveTrue()).thenReturn(List.of(quantity, payment));

        PromotionApplicationResponse result = service.applyPromotions(
                List.of(item(ProductType.SOBRE, 3, "100"), item(ProductType.CARTA, 1, "500")),
                PaymentMethod.MERCADO_PAGO);

        assertThat(result.getTotalDiscount()).isEqualByComparingTo("150.00");
        assertThat(result.getAppliedPromotions()).hasSize(1);
        assertThat(result.getAppliedPromotions().getFirst().getPromotionId()).isEqualTo(quantity.getId());
    }

    private void assertDiscount(List<CartItem> items, String expected) {
        when(promotions.findByActiveTrue()).thenReturn(List.of(quantity));
        PromotionApplicationResponse result = service.applyPromotions(items, PaymentMethod.MERCADO_PAGO);
        assertThat(result.getTotalDiscount()).isEqualByComparingTo(expected);
        if (new BigDecimal(expected).signum() == 0) {
            assertThat(result.getAppliedPromotions()).isEmpty();
        } else {
            assertThat(result.getAppliedPromotions()).hasSize(1);
            assertThat(result.getAppliedPromotions().getFirst().getPromotionId()).isEqualTo(quantity.getId());
        }
    }

    private Promotion promotion(Long id, PromotionType type, String percentage) {
        Promotion result = new Promotion();
        result.setId(id);
        result.setName("QA unit test");
        result.setType(type);
        result.setDiscountPercentage(new BigDecimal(percentage));
        result.setActive(true);
        result.setStackable(true);
        result.setStartDate(LocalDateTime.now().minusDays(1));
        return result;
    }

    private CartItem item(ProductType type, int quantity, String price) {
        Product product = new Product();
        product.setType(type);
        product.setPrice(new BigDecimal(price));
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }

    private ProductType otherType(ProductType type) {
        return type == ProductType.CARTA ? ProductType.SOBRE : ProductType.CARTA;
    }
}