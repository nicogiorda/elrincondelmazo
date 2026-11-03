package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.controllers.config.JwtService;
import com.uade.elrincondelmazo.entity.*;
import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.enums.*;
import com.uade.elrincondelmazo.repository.*;
import com.uade.elrincondelmazo.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.MediaType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real PostgreSQL and JWT fixtures, always rolled back; never uses existing accounts or Storage. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class SellerDeactivationIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @MockitoSpyBean UserRepository users;
    @Autowired CollectionRepository collections;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository orderItems;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository cartItems;
    @Autowired JwtService jwt;
    @Autowired UserService userService;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;

    User seller;
    User buyer;
    Collection collection;
    String prefix;
    final LocalDateTime date = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void fixtures() {
        prefix = "QA_DEACTIVATE_" + UUID.randomUUID();
        seller = user("seller");
        buyer = user("buyer");
        collection = new Collection();
        collection.setName(prefix);
        collection.setDescription("Transactional QA fixture");
        collection = collections.saveAndFlush(collection);
    }

    User user(String suffix) {
        return users.saveAndFlush(User.builder().email(prefix + suffix + "@example.invalid")
                .passwordHash("qa-not-a-login-password").firstName("QA").lastName(suffix)
                .role(Role.USUARIO).active(true).createdAt(date).build());
    }

    Product product(User owner, ProductStatus state) {
        Product p = new Product();
        p.setName(prefix + UUID.randomUUID());
        p.setDescription("Preserve description");
        p.setPrice(new BigDecimal("12.50"));
        p.setStock(state == ProductStatus.AGOTADO ? 0 : 7);
        p.setStatus(state);
        p.setType(ProductType.SOBRE);
        p.setCreated_at(date);
        p.setImageUrls(List.of("https://example.invalid/qa-first.png", "https://example.invalid/qa-second.png"));
        p.setSeller(owner);
        p.setCollection(collection);
        return products.saveAndFlush(p);
    }

    String auth(User user) { return "Bearer " + jwt.generateToken(user); }

    void deactivate() throws Exception {
        mvc.perform(put("/users/me/deactivate").header("Authorization", auth(seller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seller.getId().intValue()))
                .andExpect(jsonPath("$.email").value(seller.getEmail()))
                .andExpect(jsonPath("$.role").value("USUARIO"))
                .andExpect(jsonPath("$.active").value(false));
    }

    // Read SQL rather than Hibernate's identity map, including every non-status column.
    String productData(long id) {
        return jdbc.queryForObject("SELECT (to_jsonb(p) - 'status')::text FROM product p WHERE id=?", String.class, id);
    }

    String row(String table, long id) {
        return jdbc.queryForObject("SELECT row_to_json(t)::text FROM " + table + " t WHERE id=?", String.class, id);
    }

    @Test
    void withdrawsBothVisibleStatesPreservingEveryOtherProductFieldAndImages() throws Exception {
        Map<Long, String> before = new LinkedHashMap<>();
        Map<Long, List<String>> images = new LinkedHashMap<>();
        Map<Long, ProductStatus> expected = new LinkedHashMap<>();
        for (ProductStatus state : ProductStatus.values()) {
            Product p = product(seller, state);
            before.put(p.getId(), productData(p.getId()));
            expected.put(p.getId(), state == ProductStatus.ELIMINADO ? state : ProductStatus.INACTIVO);
            images.put(p.getId(), List.copyOf(p.getImageUrls()));
        }
        String account = jdbc.queryForObject("SELECT (to_jsonb(u)-'active')::text FROM users u WHERE id=?",
                String.class, seller.getId());
        deactivate();
        assertThat(jdbc.queryForObject("SELECT active FROM users WHERE id=?", Boolean.class, seller.getId())).isFalse();
        assertThat(jdbc.queryForObject("SELECT (to_jsonb(u)-'active')::text FROM users u WHERE id=?",
                String.class, seller.getId())).isEqualTo(account);
        em.clear();
        for (Map.Entry<Long, String> entry : before.entrySet()) {
            Product p = products.findById(entry.getKey()).orElseThrow();
            assertThat(p.getStatus()).isEqualTo(expected.get(p.getId()));
            assertThat(productData(p.getId())).isEqualTo(entry.getValue());
            assertThat(p.getImageUrls()).containsExactlyElementsOf(images.get(p.getId()));
        }
    }

    @Test
    void catalogExcludesSellerButPreservesAnotherSellersVisibleProducts() throws Exception {
        for (ProductStatus state : ProductStatus.values()) product(seller, state);
        Product otherActive = product(buyer, ProductStatus.ACTIVO);
        Product otherExhausted = product(buyer, ProductStatus.AGOTADO);
        String activeBefore = row("product", otherActive.getId());
        String exhaustedBefore = row("product", otherExhausted.getId());
        deactivate();
        mvc.perform(get("/products").param("search", prefix))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].sellerId").value(buyer.getId().intValue()))
                .andExpect(jsonPath("$.content[1].sellerId").value(buyer.getId().intValue()));
        assertThat(row("product", otherActive.getId())).isEqualTo(activeBefore);
        assertThat(row("product", otherExhausted.getId())).isEqualTo(exhaustedBefore);
        assertThat(users.findById(buyer.getId()).orElseThrow().getActive()).isTrue();
    }

    @Test
    void buyerCannotAddWithdrawnProductToCart() throws Exception {
        Product p = product(seller, ProductStatus.ACTIVO);
        deactivate();
        mvc.perform(post("/carts/items").header("Authorization", auth(buyer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":1}".formatted(p.getId())))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cart_item WHERE product_id=?", Long.class, p.getId())).isZero();
    }

    @Test
    void existingCartCannotCheckoutAndRemainsRemovableWithoutChangingStockOrCreatingOrder() throws Exception {
        Product p = product(seller, ProductStatus.ACTIVO);
        Cart cart = new Cart();
        cart.setUser(buyer);
        cart.setCreatedAt(date);
        cart = carts.saveAndFlush(cart);
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(p);
        item.setQuantity(2);
        item = cartItems.saveAndFlush(item);
        String cartBefore = row("cart", cart.getId());
        String itemBefore = row("cart_item", item.getId());
        String productBefore = productData(p.getId());
        long orderCount = orders.count();
        long orderItemCount = orderItems.count();
        deactivate();
        assertThat(row("cart", cart.getId())).isEqualTo(cartBefore);
        assertThat(row("cart_item", item.getId())).isEqualTo(itemBefore);
        mvc.perform(post("/orders").header("Authorization", auth(buyer))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\":\"MERCADO_PAGO\",\"expectedTotal\":100,\"requestKey\":\""+UUID.randomUUID()+"\"}"))
                .andExpect(status().isBadRequest());
        assertThat(orders.count()).isEqualTo(orderCount);
        assertThat(orderItems.count()).isEqualTo(orderItemCount);
        assertThat(productData(p.getId())).isEqualTo(productBefore);
        assertThat(row("cart_item", item.getId())).isEqualTo(itemBefore);
        mvc.perform(delete("/carts/items/" + item.getId()).header("Authorization", auth(buyer)))
                .andExpect(status().isOk());
    }

    @Test
    void existingSellerJwtStopsAuthorizingPrivateEndpoints() throws Exception {
        String token = auth(seller);
        mvc.perform(get("/users/me").header("Authorization", token)).andExpect(status().isOk());
        deactivate();
        for (String path : List.of("/users/me", "/products/mine", "/carts", "/orders")) {
            mvc.perform(get(path).header("Authorization", token)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void preservesHistoricalOrderPricesReviewsAndPromotions() throws Exception {
        Product p = product(seller, ProductStatus.ACTIVO);
        Order order = new Order();
        order.setUser(buyer);
        order.setOrderDate(date);
        order.setState(StateOrder.PAGO);
        order.setPaymentMethod(PaymentMethod.MERCADO_PAGO);
        order.setSubtotal(new BigDecimal("7.50"));
        order.setTotalDiscount(new BigDecimal("0.75"));
        order.setTotal(new BigDecimal("6.75"));
        order = orders.saveAndFlush(order);
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(p);
        item.setSeller(seller);
        item.setQuantity(1);
        item.setPrice(new BigDecimal("7.50"));
        item = orderItems.saveAndFlush(item);
        Promotion promo = new Promotion();
        promo.setName(prefix);
        promo.setType(PromotionType.PRODUCTO);
        promo.setProduct(p);
        promo.setDiscountPercentage(new BigDecimal("10"));
        promo.setStartDate(date);
        promo.setActive(false);
        em.persist(promo);
        OrderPromotion applied = new OrderPromotion();
        applied.setOrder(order);
        applied.setPromotion(promo);
        applied.setDiscountApplied(new BigDecimal("0.75"));
        em.persist(applied);
        Review review = new Review();
        review.setProduct(p);
        review.setUser(buyer);
        review.setRating(5);
        review.setComment("QA historical review");
        review.setCreatedAt(date);
        em.persist(review);
        em.flush();
        Map<String, Long> ids = Map.of("orders", order.getId(), "order_item", item.getId(),
                "promotion", promo.getId(), "order_promotion", applied.getId(), "reviews", review.getId());
        Map<String, String> before = new LinkedHashMap<>();
        ids.forEach((table, id) -> before.put(table, row(table, id)));
        deactivate();
        ids.forEach((table, id) -> assertThat(row(table, id)).isEqualTo(before.get(table)));
        mvc.perform(get("/orders/" + order.getId()).header("Authorization", auth(buyer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].price").value(7.5))
                .andExpect(jsonPath("$.total").value(6.75))
                .andExpect(jsonPath("$.promotions[0].discountApplied").value(0.75));
    }

    @Test
    void existingPublicIdContractRemainsUnchanged() throws Exception {
        Product p = product(seller, ProductStatus.ACTIVO);
        Product deleted = product(seller, ProductStatus.ELIMINADO);
        deactivate();
        mvc.perform(get("/products/" + p.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVO"));
        mvc.perform(get("/products/" + deleted.getId())).andExpect(status().isNotFound());
    }

    @Test
    void accountWithoutPublicationsCanStillBeDeactivated() throws Exception {
        deactivate();
        assertThat(users.findById(seller.getId()).orElseThrow().getActive()).isFalse();
    }

    @Test
    void anonymousRequestCannotDeactivateAccount() throws Exception {
        Product p = product(seller, ProductStatus.ACTIVO);
        mvc.perform(put("/users/me/deactivate")).andExpect(status().isUnauthorized());
        assertThat(users.findById(seller.getId()).orElseThrow().getActive()).isTrue();
        assertThat(row("product", p.getId())).contains("ACTIVO");
    }

    @Test
    void failureAfterBothSqlUpdatesMarksTransactionForRollbackAndLeavesNoFixtureRows() {
        Product active = product(seller, ProductStatus.ACTIVO);
        Product exhausted = product(seller, ProductStatus.AGOTADO);
        long sellerId = seller.getId();
        long activeId = active.getId();
        long exhaustedId = exhausted.getId();
        // Inject a failure at the final reload, after flush + bulk SQL, without changing production code.
        doAnswer(invocation -> {
            assertThat(jdbc.queryForObject("SELECT active FROM users WHERE id=?", Boolean.class, sellerId)).isFalse();
            assertThat(jdbc.queryForList("SELECT status FROM product WHERE seller_id=?", String.class, sellerId))
                    .containsExactlyInAnyOrder("INACTIVO", "INACTIVO");
            throw new IllegalStateException("QA controlled failure after both SQL updates");
        }).when(users).findById(sellerId);
        TransactionStatus participating = transactions.getTransaction(new DefaultTransactionDefinition());
        try {
            assertThat(participating.isRollbackOnly()).isFalse();
            assertThatThrownBy(() -> userService.deactivateUser(seller.getEmail()))
                    .isInstanceOf(InvalidDataAccessApiUsageException.class)
                    .hasRootCauseInstanceOf(IllegalStateException.class).hasMessageContaining("QA controlled failure");
            assertThat(participating.isRollbackOnly()).isTrue();
        } finally {
            transactions.rollback(participating);
            TestTransaction.flagForRollback();
            TestTransaction.end();
        }
        // New connection after rollback: both account and publications created by this test disappeared.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE id=?", Long.class, sellerId)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM product WHERE id IN (?,?)", Long.class, activeId, exhaustedId)).isZero();
    }
}
