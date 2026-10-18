package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.controllers.config.JwtService;
import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.Order;
import com.uade.elrincondelmazo.entity.OrderItem;
import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.enums.*;
import com.uade.elrincondelmazo.exception.InvalidProductException;
import com.uade.elrincondelmazo.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

/**
 * Integración con PostgreSQL y filtros JWT reales. Sólo crea fixtures QA propios:
 * cada test hace rollback; no utiliza ni modifica registros existentes.
 * Requiere la configuración local de desarrollo, nunca una base productiva.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class ProductPublicationIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired CollectionRepository collections;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository orderItems;
    @Autowired JwtService jwt;
    @Autowired EntityManager em;

    User owner;
    User other;
    User admin;
    Collection collection;
    String prefix;
    final LocalDateTime date = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void fixtures() {
        prefix = "QA_PUBLICATION_TEST_" + UUID.randomUUID();
        owner = user("owner", Role.USUARIO);
        other = user("other", Role.USUARIO);
        admin = user("admin", Role.ADMIN);
        collection = new Collection();
        collection.setName(prefix);
        collection.setDescription("Fixture transaccional");
        collection = collections.saveAndFlush(collection);
    }

    User user(String suffix, Role role) {
        return users.saveAndFlush(User.builder()
                .email(prefix + suffix + "@example.invalid")
                .passwordHash("qa-test-not-a-login-password")
                .firstName("QA").lastName(suffix).role(role)
                .active(true).createdAt(date).build());
    }

    Product product(User seller, ProductStatus status, LocalDateTime created) {
        Product p = new Product();
        p.setName(prefix + "_" + UUID.randomUUID());
        p.setDescription("Fixture QA descartable");
        p.setPrice(new BigDecimal("10.00"));
        p.setType(ProductType.CARTA);
        p.setImageUrls(List.of("https://example.invalid/qa.png"));
        p.setStock(status == ProductStatus.AGOTADO ? 0 : 3);
        p.setStatus(status);
        p.setCreated_at(created);
        p.setSeller(seller);
        p.setCollection(collection);
        return products.saveAndFlush(p);
    }

    String auth(User user) {
        return "Bearer " + jwt.generateToken(user);
    }

    String request(int stock) {
        return """
                {"name":"QA editado","description":"Edicion QA",
                 "price":12.50,"type":"CARTA","imageUrls":["https://example.invalid/qa.png"],
                 "stock":%d,"collectionId":%d}
                """.formatted(stock, collection.getId());
    }

    Product reload(Long id) {
        em.flush();
        em.clear();
        return products.findById(id).orElseThrow();
    }

    @Test
    void mineRequiresJwtBeforePublicProductMatcher() throws Exception {
        mvc.perform(get("/products/mine")).andExpect(status().isUnauthorized());
        mvc.perform(get("/products")).andExpect(status().isOk());
    }

    @Test
    void mineIncludesOnlyOwnNonDeletedPublications() throws Exception {
        Product active = product(owner, ProductStatus.ACTIVO, date);
        Product paused = product(owner, ProductStatus.INACTIVO, date);
        Product exhausted = product(owner, ProductStatus.AGOTADO, date);
        product(owner, ProductStatus.ELIMINADO, date);
        product(other, ProductStatus.ACTIVO, date.plusDays(1));

        mvc.perform(get("/products/mine").header("Authorization", auth(owner))
                        .param("sellerId", other.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content[0].id").value(exhausted.getId().intValue()))
                .andExpect(jsonPath("$.content[1].id").value(paused.getId().intValue()))
                .andExpect(jsonPath("$.content[2].id").value(active.getId().intValue()))
                .andExpect(jsonPath("$.content[0].sellerId").value(owner.getId().intValue()));
    }

    @Test
    void minePaginatesInDatabaseWithStableDateAndIdOrder() throws Exception {
        Product older = product(owner, ProductStatus.ACTIVO, date);
        Product newer = product(owner, ProductStatus.INACTIVO, date.plusDays(1));
        Product sameDateHigherId = product(owner, ProductStatus.AGOTADO, date.plusDays(1));
        mvc.perform(get("/products/mine?page=0&size=2").header("Authorization", auth(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(sameDateHigherId.getId().intValue()))
                .andExpect(jsonPath("$.content[1].id").value(newer.getId().intValue()));
        mvc.perform(get("/products/mine?page=1&size=2").header("Authorization", auth(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(older.getId().intValue()));
    }

    @Test
    void mineCanReadBeyondFirstHundredProducts() throws Exception {
        // Un INSERT SQL transaccional permite comprobar paginación real sin 105 requests.
        em.createNativeQuery("""
                INSERT INTO product (name, description, price, type, stock, status, created_at, seller_id, collection_id)
                SELECT concat(:prefix, '-', g), 'QA pagination', 10, 'CARTA', 3, 'ACTIVO',
                       :created, :seller, :collection
                FROM generate_series(1, 105) AS g
                """).setParameter("prefix", prefix).setParameter("created", date)
                .setParameter("seller", owner.getId()).setParameter("collection", collection.getId())
                .executeUpdate();
        mvc.perform(get("/products/mine?page=100&size=1").header("Authorization", auth(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(105))
                .andExpect(jsonPath("$.number").value(100))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void mineRejectsInvalidPagination() throws Exception {
        for (String query : List.of("page=-1", "size=0", "size=-1", "size=201", "page=abc", "size=abc")) {
            mvc.perform(get("/products/mine?" + query).header("Authorization", auth(owner)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void pauseAndResumePersistWithoutChangingOtherAttributes() throws Exception {
        Product p = product(owner, ProductStatus.ACTIVO, date);
        mvc.perform(patch("/products/" + p.getId() + "/pause").header("Authorization", auth(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVO"));
        Product paused = reload(p.getId());
        assertThat(paused.getStatus()).isEqualTo(ProductStatus.INACTIVO);
        assertThat(paused.getStock()).isEqualTo(3);
        assertThat(paused.getPrice()).isEqualByComparingTo("10");
        assertThat(paused.getCreated_at()).isEqualTo(date);
        assertThat(paused.getDescription()).isEqualTo("Fixture QA descartable");
        mvc.perform(patch("/products/" + p.getId() + "/resume").header("Authorization", auth(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVO"));
        assertThat(reload(p.getId()).getStatus()).isEqualTo(ProductStatus.ACTIVO);
    }

    @Test
    void repeatedPauseAndResumeAreIdempotent() throws Exception {
        Product p = product(owner, ProductStatus.ACTIVO, date);
        for (int i = 0; i < 2; i++) {
            mvc.perform(patch("/products/" + p.getId() + "/pause").header("Authorization", auth(owner)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVO"));
        }
        for (int i = 0; i < 2; i++) {
            mvc.perform(patch("/products/" + p.getId() + "/resume").header("Authorization", auth(owner)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVO"));
        }
        assertThat(reload(p.getId()).getStock()).isEqualTo(3);
    }

    @Test
    void exhaustedProductsCannotBePausedOrResumed() throws Exception {
        Product p = product(owner, ProductStatus.AGOTADO, date);
        mvc.perform(patch("/products/" + p.getId() + "/pause").header("Authorization", auth(owner)))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/products/" + p.getId() + "/resume").header("Authorization", auth(owner)))
                .andExpect(status().isBadRequest());
        assertThat(reload(p.getId()).getStatus()).isEqualTo(ProductStatus.AGOTADO);
    }

    @Test
    void pausedWithoutStockCannotBeResumed() throws Exception {
        Product p = product(owner, ProductStatus.INACTIVO, date);
        p.setStock(0);
        products.saveAndFlush(p);
        mvc.perform(patch("/products/" + p.getId() + "/resume").header("Authorization", auth(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException()).isInstanceOf(InvalidProductException.class).hasMessageContaining("sin stock"));
        assertThat(reload(p.getId()).getStatus()).isEqualTo(ProductStatus.INACTIVO);
    }

    @Test
    void foreignSellerCannotPauseResumeDeleteOrEdit() throws Exception {
        Product p = product(owner, ProductStatus.ACTIVO, date);
        mvc.perform(patch("/products/" + p.getId() + "/pause").header("Authorization", auth(other)))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/products/" + p.getId() + "/resume").header("Authorization", auth(other)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/products/" + p.getId()).header("Authorization", auth(other)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/products/" + p.getId()).header("Authorization", auth(other))
                        .contentType(MediaType.APPLICATION_JSON).content(request(3)))
                .andExpect(status().isForbidden());
        assertThat(reload(p.getId()).getStatus()).isEqualTo(ProductStatus.ACTIVO);
    }

    @Test
    void writesRequireJwt() throws Exception {
        Product p = product(owner, ProductStatus.ACTIVO, date);
        mvc.perform(patch("/products/" + p.getId() + "/pause")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/products/" + p.getId() + "/resume")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/products/" + p.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanManageAnotherSellersPublication() throws Exception {
        Product p = product(owner, ProductStatus.ACTIVO, date);
        mvc.perform(patch("/products/" + p.getId() + "/pause").header("Authorization", auth(admin)))
                .andExpect(status().isOk());
        mvc.perform(patch("/products/" + p.getId() + "/resume").header("Authorization", auth(admin)))
                .andExpect(status().isOk());
        mvc.perform(delete("/products/" + p.getId()).header("Authorization", auth(admin)))
                .andExpect(status().isNoContent());
        assertThat(reload(p.getId()).getStatus()).isEqualTo(ProductStatus.ELIMINADO);
    }

    @Test
    void deleteIsLogicalIdempotentAndRemovesProductFromMineAndCatalog() throws Exception {
        Product p = product(owner, ProductStatus.ACTIVO, date);
        long before = products.count();
        mvc.perform(delete("/products/" + p.getId()).header("Authorization", auth(owner)))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(delete("/products/" + p.getId()).header("Authorization", auth(owner)))
                .andExpect(status().isNoContent());
        assertThat(reload(p.getId()).getStatus()).isEqualTo(ProductStatus.ELIMINADO);
        assertThat(products.count()).isEqualTo(before);
        mvc.perform(get("/products").param("search", prefix)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/products/mine").header("Authorization", auth(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/products/" + p.getId())).andExpect(status().isNotFound());
    }

    @Test
    void publicCatalogIncludesExhaustedButNotPausedOrDeleted() throws Exception {
        product(owner, ProductStatus.ACTIVO, date);
        product(owner, ProductStatus.AGOTADO, date);
        product(owner, ProductStatus.INACTIVO, date);
        product(owner, ProductStatus.ELIMINADO, date);
        mvc.perform(get("/products").param("search", prefix)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void deletedOrMissingProductsCannotPauseOrResume() throws Exception {
        Product p = product(owner, ProductStatus.ELIMINADO, date);
        for (long id : List.of(p.getId(), Long.MAX_VALUE)) {
            mvc.perform(patch("/products/" + id + "/pause").header("Authorization", auth(owner)))
                    .andExpect(status().isNotFound());
            mvc.perform(patch("/products/" + id + "/resume").header("Authorization", auth(owner)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void editingPausedWithPositiveStockKeepsItPaused() throws Exception {
        Product p = product(owner, ProductStatus.INACTIVO, date);
        mvc.perform(put("/products/" + p.getId()).header("Authorization", auth(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(request(5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVO"));
        Product updated = reload(p.getId());
        assertThat(updated.getStock()).isEqualTo(5);
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.INACTIVO);
    }

    @Test
    void logicalDeletePreservesOrderHistoryAndItsHistoricalPrice() throws Exception {
        Product p = product(owner, ProductStatus.ACTIVO, date);
        Order order = new Order();
        order.setUser(other);
        order.setOrderDate(date);
        order.setState(StateOrder.PAGO);
        order.setPaymentMethod(PaymentMethod.MERCADO_PAGO);
        order.setSubtotal(new BigDecimal("7.50"));
        order.setTotalDiscount(BigDecimal.ZERO);
        order.setTotal(new BigDecimal("7.50"));
        order = orders.saveAndFlush(order);
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(p);
        item.setSeller(owner);
        item.setQuantity(1);
        item.setPrice(new BigDecimal("7.50"));
        item = orderItems.saveAndFlush(item);

        mvc.perform(delete("/products/" + p.getId()).header("Authorization", auth(owner)))
                .andExpect(status().isNoContent());
        em.flush();
        em.clear();
        assertThat(products.existsById(p.getId())).isTrue();
        assertThat(orderItems.findById(item.getId())).isPresent();
        mvc.perform(get("/orders/" + order.getId()).header("Authorization", auth(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(p.getId().intValue()))
                .andExpect(jsonPath("$.items[0].price").value(7.5));
    }
}
