package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.Product;
import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.enums.ProductStatus;
import com.uade.elrincondelmazo.enums.ProductType;
import com.uade.elrincondelmazo.enums.Role;
import com.uade.elrincondelmazo.repository.CollectionRepository;
import com.uade.elrincondelmazo.repository.ProductRepository;
import com.uade.elrincondelmazo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET público real, con fixtures aislados por nombre y rollback de cada test. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
@Rollback
class ProductCatalogOrderingIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired CollectionRepository collections;
    @Autowired EntityManager em;

    final LocalDateTime date = LocalDateTime.of(2026, 1, 1, 12, 0);
    final JsonMapper json = JsonMapper.builder().build();
    String prefix;
    User seller;
    Collection collection;

    @BeforeEach
    void fixtures() {
        prefix = "QA_CATALOG_ORDER_" + UUID.randomUUID() + "_";
        seller = users.save(User.builder().email(prefix + "@example.invalid")
                .passwordHash("qa-test-not-a-login-password").firstName("QA").lastName("Catalog")
                .role(Role.USUARIO).active(true).createdAt(date).build());
        collection = collection("main");
    }

    @Test
    void publicCatalogOrdersByCreationDescendingInsteadOfInsertionOrder() throws Exception {
        Product oldest = product("oldest", date, ProductStatus.ACTIVO);
        Product newest = product("newest", date.plusDays(2), ProductStatus.ACTIVO);
        Product middle = product("middle", date.plusDays(1), ProductStatus.ACTIVO);

        assertThat(ids(page(0))).containsExactly(newest.getId(), middle.getId(), oldest.getId());
    }

    @Test
    void identicalCreationDatesAreOrderedByIdDescending() throws Exception {
        Product first = product("first", date, ProductStatus.ACTIVO);
        Product second = product("second", date, ProductStatus.ACTIVO);
        Product third = product("third", date, ProductStatus.ACTIVO);

        assertThat(ids(page(0))).containsExactly(third.getId(), second.getId(), first.getId());
    }

    @Test
    void consecutiveNineItemPagesKeepGlobalOrderWithoutDuplicates() throws Exception {
        List<Product> fixtures = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            fixtures.add(product("page-" + i, date.plusDays(i % 4), ProductStatus.ACTIVO));
        }
        List<Long> expected = fixtures.stream()
                .sorted(Comparator.comparing(Product::getCreated_at).thenComparing(Product::getId).reversed())
                .map(Product::getId).toList();

        JsonNode first = page(0);
        JsonNode second = page(1);
        assertThat(first.get("number").asInt()).isZero();
        assertThat(second.get("number").asInt()).isEqualTo(1);
        for (JsonNode result : List.of(first, second)) {
            assertThat(result.get("totalElements").asLong()).isEqualTo(18);
            assertThat(result.get("totalPages").asInt()).isEqualTo(2);
            assertThat(ids(result)).hasSize(9);
        }
        List<Long> combined = new ArrayList<>(ids(first));
        combined.addAll(ids(second));
        assertThat(combined).doesNotHaveDuplicates().containsExactlyElementsOf(expected);
        assertThat(ids(page(0))).containsExactlyElementsOf(ids(first));
        assertThat(ids(page(1))).containsExactlyElementsOf(ids(second));
    }

    @Test
    void combinedSearchTypeCollectionAndPriceFiltersPreserveOrder() throws Exception {
        Product low = product("MATCH-low", date, ProductStatus.ACTIVO);
        Product high = product("MATCH-high", date.plusDays(1), ProductStatus.ACTIVO);
        Product middle = product("MATCH-middle", date.plusDays(2), ProductStatus.ACTIVO);
        low.setPrice(new BigDecimal("20"));
        high.setPrice(new BigDecimal("40"));
        middle.setPrice(new BigDecimal("30"));
        product("UNRELATED", date.plusDays(9), ProductStatus.ACTIVO);
        product("MATCH-wrong-type", date.plusDays(8), ProductStatus.ACTIVO).setType(ProductType.CARTA);
        product("MATCH-wrong-collection", date.plusDays(7), ProductStatus.ACTIVO).setCollection(collection("other"));
        product("MATCH-too-cheap", date.plusDays(6), ProductStatus.ACTIVO).setPrice(new BigDecimal("19"));
        product("MATCH-too-expensive", date.plusDays(5), ProductStatus.ACTIVO).setPrice(new BigDecimal("41"));

        JsonNode result = request(get("/products").param("search", prefix + "MATCH")
                .param("type", "SOBRE").param("collectionId", collection.getId().toString())
                .param("minPrice", "20").param("maxPrice", "40")
                .param("page", "0").param("size", "9"));

        assertThat(result.get("totalElements").asLong()).isEqualTo(3);
        assertThat(ids(result)).containsExactly(middle.getId(), high.getId(), low.getId());
    }

    @Test
    void pausedAndDeletedProductsRemainExcludedEvenWhenNewer() throws Exception {
        Product visible = product("visible", date, ProductStatus.ACTIVO);
        product("paused", date.plusDays(2), ProductStatus.INACTIVO);
        product("deleted", date.plusDays(3), ProductStatus.ELIMINADO);

        JsonNode result = page(0);
        assertThat(result.get("totalElements").asLong()).isEqualTo(1);
        assertThat(ids(result)).containsExactly(visible.getId());
    }

    @ParameterizedTest
    @EnumSource(value = ProductStatus.class, names = {"ACTIVO", "AGOTADO"})
    void activeAndExhaustedProductsRemainPublic(ProductStatus status) throws Exception {
        Product visible = product("visible", date, status);

        JsonNode result = page(0);
        assertThat(ids(result)).containsExactly(visible.getId());
        assertThat(result.get("content").get(0).get("status").asText()).isEqualTo(status.name());
    }

    private Collection collection(String suffix) {
        Collection result = new Collection();
        result.setName(prefix + suffix);
        return collections.save(result);
    }

    private Product product(String suffix, LocalDateTime created, ProductStatus status) {
        Product result = new Product();
        result.setName(prefix + suffix);
        result.setDescription("Fixture QA transaccional");
        result.setPrice(new BigDecimal("30"));
        result.setType(ProductType.SOBRE);
        result.setImageUrls(List.of("https://example.invalid/qa.png"));
        result.setStock(status == ProductStatus.AGOTADO ? 0 : 3);
        result.setStatus(status);
        result.setCreated_at(created);
        result.setSeller(seller);
        result.setCollection(collection);
        return products.save(result);
    }

    private JsonNode page(int number) throws Exception {
        return request(get("/products").param("search", prefix)
                .param("page", String.valueOf(number)).param("size", "9"));
    }

    private JsonNode request(MockHttpServletRequestBuilder request) throws Exception {
        em.flush();
        em.clear();
        String response = mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private List<Long> ids(JsonNode page) {
        return StreamSupport.stream(page.get("content").spliterator(), false)
                .map(product -> product.get("id").asLong()).toList();
    }
}