package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.controllers.config.JwtService;
import com.uade.elrincondelmazo.entity.Cart;
import com.uade.elrincondelmazo.entity.CartItem;
import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.Order;
import com.uade.elrincondelmazo.entity.OrderPromotion;
import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.entity.Promotion;
import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.enums.*;
import com.uade.elrincondelmazo.repository.*;
import com.uade.elrincondelmazo.service.PromotionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real engine, repositories, JWT and PostgreSQL. All QA fixtures and orders roll back. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class PromotionCheckoutIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired CollectionRepository collections;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository cartItems;
    @Autowired PromotionRepository promotions;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository orderItems;
    @Autowired OrderPromotionRepository orderPromotions;
    @Autowired PromotionService engine;
    @Autowired JwtService jwt;
    @Autowired EntityManager em;

    User buyer;
    Product product;
    CartItem item;
    String prefix;

    @BeforeEach
    void fixtures() {
        prefix = "QA_STACKABLE_" + UUID.randomUUID();
        User seller = user("seller");
        buyer = user("buyer");
        Collection collection = new Collection();
        collection.setName(prefix);
        collection.setDescription("Transactional QA fixture");
        collection = collections.saveAndFlush(collection);
        product = new Product();
        product.setName(prefix);
        product.setDescription("QA discount regression");
        product.setPrice(new BigDecimal("100.00"));
        product.setType(ProductType.SOBRE);
        product.setImageUrls(List.of("https://example.invalid/qa.png"));
        product.setStock(10);
        product.setStatus(ProductStatus.ACTIVO);
        product.setSeller(seller);
        product.setCollection(collection);
        product.setCreated_at(LocalDateTime.now());
        product = products.saveAndFlush(product);
        Cart cart = new Cart();
        cart.setUser(buyer);
        cart.setCreatedAt(LocalDateTime.now());
        cart = carts.saveAndFlush(cart);
        item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(1);
        item = cartItems.saveAndFlush(item);
        // Existing promotions must not affect the isolated product/collection; never deactivate them.
        assertThat(engine.applyPromotions(List.of(item), PaymentMethod.MERCADO_PAGO).getTotalDiscount())
                .as("Existing promotions must not affect the QA fixture").isZero();
    }

    @ParameterizedTest
    @CsvSource({"30,30,10,60,true", "5,5,25,25,false", "80,80,70,100,true", "15,15,30,30,false", "80,80,100,100,false"})
    void checkoutStoresOnlyWinningPromotionsWithExactDiscounts(String a, String b, String c,
                                                               String expected, boolean stackablesWin) throws Exception {
        Promotion first = promotion("A", a, true);
        Promotion second = promotion("B", b, true);
        Promotion exclusive = promotion("C", c, false);
        promotion("D same exclusive saving, higher ID", c, false);
        BigDecimal discount = new BigDecimal(expected);
        BigDecimal total = new BigDecimal("100.00").subtract(discount);
        var result = mvc.perform(post("/orders").header("Authorization", "Bearer " + jwt.generateToken(buyer))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\":\"MERCADO_PAGO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(100))
                .andExpect(jsonPath("$.totalDiscount").value(discount.doubleValue()))
                .andExpect(jsonPath("$.total").value(total.doubleValue()))
                .andExpect(jsonPath("$.promotions.length()").value(stackablesWin ? 2 : 1))
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        assertThat(location).startsWith("/orders/");
        Long orderId = Long.valueOf(location.substring("/orders/".length()));
        em.flush();
        em.clear();
        Order saved = orders.findById(orderId).orElseThrow();
        assertThat(saved.getTotalDiscount()).isEqualByComparingTo(discount);
        assertThat(saved.getTotal()).isEqualByComparingTo(total).isNotNegative();
        assertThat(saved.getSubtotal()).isEqualByComparingTo("100.00");
        List<OrderPromotion> applied = orderPromotions.findByOrderId(orderId);
        assertThat(applied).extracting(p -> p.getPromotion().getId())
                .containsExactlyInAnyOrderElementsOf(stackablesWin
                        ? List.of(first.getId(), second.getId()) : List.of(exclusive.getId()));
        assertThat(applied.stream().map(OrderPromotion::getDiscountApplied).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(saved.getTotalDiscount());
        for (OrderPromotion p : applied) {
            BigDecimal expectedPart = p.getPromotion().getId().equals(first.getId())
                    ? new BigDecimal(a) : p.getPromotion().getId().equals(second.getId())
                    ? discount.subtract(new BigDecimal(a)) : discount;
            assertThat(p.getDiscountApplied()).isEqualByComparingTo(expectedPart);
        }
        assertThat(orderItems.findByOrderId(orderId)).singleElement().satisfies(i -> {
            assertThat(i.getProduct().getId()).isEqualTo(product.getId());
            assertThat(i.getPrice()).isEqualByComparingTo("100.00");
            assertThat(i.getQuantity()).isEqualTo(1);
        });
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(9);
        assertThat(cartItems.findByCart_Id(item.getCart().getId())).isEmpty();
        mvc.perform(get(location).header("Authorization", "Bearer " + jwt.generateToken(buyer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalDiscount").value(discount.doubleValue()))
                .andExpect(jsonPath("$.promotions.length()").value(stackablesWin ? 2 : 1));
    }

    private User user(String suffix) {
        return users.saveAndFlush(User.builder().email(prefix + suffix + "@example.invalid")
                .passwordHash("qa-not-a-login-password").firstName("QA").lastName(suffix).role(Role.USUARIO)
                .active(true).createdAt(LocalDateTime.now()).build());
    }

    private Promotion promotion(String name, String percentage, boolean stackable) {
        Promotion p = new Promotion();
        p.setName(prefix + name);
        p.setType(PromotionType.PRODUCTO);
        p.setProduct(product);
        p.setDiscountPercentage(new BigDecimal(percentage));
        p.setStackable(stackable);
        p.setActive(true);
        p.setStartDate(LocalDateTime.now().minusDays(1));
        return promotions.saveAndFlush(p);
    }
}
