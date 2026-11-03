package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.controllers.config.JwtService;
import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.entity.Promotion;
import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.enums.PromotionType;
import com.uade.elrincondelmazo.enums.Role;
import com.uade.elrincondelmazo.repository.CollectionRepository;
import com.uade.elrincondelmazo.repository.PromotionRepository;
import com.uade.elrincondelmazo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses the configured PostgreSQL; fixtures and API writes roll back after each test. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class PromotionAdministrationIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired PromotionRepository promotions;
    @Autowired CollectionRepository collections;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;

    String prefix;
    String admin;
    Collection collection;

    @BeforeEach
    void fixtures() {
        prefix = "QA_PROMO_" + UUID.randomUUID();
        admin = token(Role.ADMIN);
        collection = new Collection();
        collection.setName(prefix);
        collection.setDescription("Transactional collection fixture");
        collection = collections.saveAndFlush(collection);
    }

    @Test
    void publicListIsAnonymousAndOnlyIncludesActiveCurrentPromotions() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Promotion current = promotion("current", now.minusDays(1), now.plusDays(1), true);
        Promotion unlimited = promotion("unlimited", now.minusDays(1), null, true);
        Promotion future = promotion("future", now.plusDays(1), null, true);
        Promotion expired = promotion("expired", now.minusDays(2), now.minusDays(1), true);
        Promotion disabled = promotion("disabled", now.minusDays(1), null, false);
        mvc.perform(get("/promotions/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(current.getName())))
                .andExpect(jsonPath("$[*].name", hasItem(unlimited.getName())))
                .andExpect(jsonPath("$[*].name", not(hasItem(future.getName()))))
                .andExpect(jsonPath("$[*].name", not(hasItem(expired.getName()))))
                .andExpect(jsonPath("$[*].name", not(hasItem(disabled.getName()))))
                .andExpect(jsonPath("$[?(@.name == '" + current.getName() + "')].discountPercentage", hasItem(13)))
                .andExpect(jsonPath("$[?(@.name == '" + current.getName() + "')].collectionName", hasItem(prefix)));
    }

    @Test
    void repositoryIncludesExactStartAndEndBoundaries() {
        LocalDateTime boundary = LocalDateTime.of(2040, 1, 1, 12, 0);
        Promotion starts = promotion("starts", boundary, null, true);
        Promotion ends = promotion("ends", boundary.minusDays(1), boundary, true);
        Promotion later = promotion("later", boundary.plusSeconds(1), null, true);
        Promotion earlier = promotion("earlier", boundary.minusDays(1), boundary.minusSeconds(1), true);
        assertThat(promotions.findPublicAt(boundary).stream().filter(p -> p.getName().startsWith(prefix)))
                .extracting(Promotion::getId).containsExactly(starts.getId(), ends.getId())
                .doesNotContain(later.getId(), earlier.getId());
    }

    @Test
    void anonymousAndRegularUsersCannotReadAdministrationOrWrite() throws Exception {
        String regular = token(Role.USUARIO);
        mvc.perform(get("/promotions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/promotions").header("Authorization", regular)).andExpect(status().isForbidden());
        mvc.perform(get("/promotions/1").header("Authorization", regular)).andExpect(status().isForbidden());
        for (var request : List.of(post("/promotions"), put("/promotions/1"),
                patch("/promotions/1/activate"), patch("/promotions/1/deactivate"), post("/promotions/public"))) {
            mvc.perform(request.header("Authorization", regular).contentType(MediaType.APPLICATION_JSON).content(body()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/promotions").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCreatesUpdatesAndTogglesUsingPersistedResponses() throws Exception {
        mvc.perform(post("/promotions").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.active").value(true)).andExpect(jsonPath("$.discountPercentage").value(13));
        Promotion created = promotions.findAll().stream().filter(p -> p.getName().equals(prefix)).findFirst().orElseThrow();
        String path = "/promotions/" + created.getId();
        mvc.perform(get("/promotions").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].name", hasItem(prefix)));
        mvc.perform(put(path).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(body().replace("\"discountPercentage\":13", "\"discountPercentage\":17")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discountPercentage").value(17));
        mvc.perform(patch(path + "/deactivate").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/promotions/public")).andExpect(jsonPath("$[*].name", not(hasItem(prefix))));
        mvc.perform(patch(path + "/activate").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
        mvc.perform(get(path).header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discountPercentage").value(17));
        mvc.perform(get("/promotions/public")).andExpect(jsonPath("$[*].name", hasItem(prefix)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"zeroDiscount", "largeDiscount", "missingCollection", "reversedDates", "zeroQuantity", "missingPayment", "missingType", "zeroAmount", "missingProduct", "blankName"})
    void rejectsInvalidCreateAndUpdate(String invalid) throws Exception {
        String content = switch (invalid) {
            case "zeroDiscount" -> body().replace("\"discountPercentage\":13", "\"discountPercentage\":0");
            case "largeDiscount" -> body().replace("\"discountPercentage\":13", "\"discountPercentage\":101");
            case "missingCollection" -> body().replace("\"collectionId\":" + collection.getId(), "\"collectionId\":null");
            case "reversedDates" -> body().replace("\"endDate\":null", "\"endDate\":\"2019-01-01T00:00:00\"");
            case "zeroQuantity" -> body().replace("\"type\":\"COLECCION\"", "\"type\":\"CANTIDAD\",\"minimumQuantity\":0");
            case "missingPayment" -> body().replace("COLECCION", "METODO_PAGO");
            case "missingType" -> body().replace("COLECCION", "TIPO_PRODUCTO");
            case "zeroAmount" -> body().replace("\"type\":\"COLECCION\"", "\"type\":\"MONTO_MINIMO\",\"minimumAmount\":0");
            case "missingProduct" -> body().replace("COLECCION", "PRODUCTO");
            default -> body().replace(prefix, " ");
        };
        Promotion existing = promotion("unchanged", LocalDateTime.now().minusDays(1), null, true);
        mvc.perform(post("/promotions").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/promotions/" + existing.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(content))
                .andExpect(status().isBadRequest());
        assertThat(existing.getName()).isEqualTo(prefix + "unchanged");
    }

    @Test
    void collectionNamesAndIdsComeFromTheRepositoryAndWritesAreRestricted() throws Exception {
        mvc.perform(get("/collections/" + collection.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(collection.getId()))
                .andExpect(jsonPath("$.name").value(prefix));
        mvc.perform(get("/collections")).andExpect(status().isOk()).andExpect(jsonPath("$[*].name", hasItem(prefix)));
        mvc.perform(post("/collections").header("Authorization", token(Role.USUARIO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"unauthorized\"}"))
                .andExpect(status().isForbidden());
    }

    private String body() {
        return """
                {"name":"%s","type":"COLECCION","discountPercentage":13,"collectionId":%d,
                 "stackable":true,"startDate":"2020-01-01T00:00:00","endDate":null}
                """.formatted(prefix, collection.getId());
    }

    private String token(Role role) {
        User user = users.saveAndFlush(User.builder().email(prefix + role + "@example.invalid")
                .passwordHash("qa-not-a-login-password").firstName("QA").lastName("Promotion")
                .role(role).active(true).createdAt(LocalDateTime.now()).build());
        return "Bearer " + jwt.generateToken(user);
    }

    private Promotion promotion(String suffix, LocalDateTime start, LocalDateTime end, boolean active) {
        Promotion promotion = new Promotion();
        promotion.setName(prefix + suffix);
        promotion.setType(PromotionType.COLECCION);
        promotion.setCollection(collection);
        promotion.setDiscountPercentage(new BigDecimal("13"));
        promotion.setStackable(true);
        promotion.setActive(active);
        promotion.setStartDate(start);
        promotion.setEndDate(end);
        return promotions.saveAndFlush(promotion);
    }
}
