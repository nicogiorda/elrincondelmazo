package com.uade.elrincondelmazo;

import com.jayway.jsonpath.JsonPath;
import com.uade.elrincondelmazo.controllers.config.JwtService;
import com.uade.elrincondelmazo.entity.*;
import com.uade.elrincondelmazo.entity.Collection;
import com.uade.elrincondelmazo.enums.*;
import com.uade.elrincondelmazo.exception.InvalidImageException;
import com.uade.elrincondelmazo.repository.*;
import com.uade.elrincondelmazo.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("audit")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class AlbumCardRegistrationIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CollectionRepository collections;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository orderItems;
    @Autowired AlbumCardRepository albumCards;
    @Autowired AlbumRepository albums;
    @Autowired AlbumSectionRepository albumSections;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository cartItems;
    @Autowired JwtService jwt;
    @MockitoBean ImageStorageService images;
    User owner, seller;
    Collection collection;
    String token;

    @BeforeEach
    void setUp() {
        owner = user("owner");
        seller = user("seller");
        token = auth(owner);
        collection = new Collection();
        collection.setName("album-integration-" + UUID.randomUUID());
        collection = collections.saveAndFlush(collection);
    }

    User user(String role) {
        return users.saveAndFlush(User.builder()
                .email("album-registration-" + role + "-" + UUID.randomUUID() + "@example.invalid")
                .passwordHash("test-only")
                .firstName("Album").lastName(role)
                .role(Role.USUARIO).active(true).createdAt(LocalDateTime.now()).build());
    }

    String auth(User user) { return "Bearer " + jwt.generateToken(user); }

    Product product(String name, ProductType type) {
        Product p = new Product();
        p.setName(name);
        p.setDescription("For purchase registration");
        p.setPrice(new BigDecimal("25.00"));
        p.setStock(20);
        p.setStatus(ProductStatus.ACTIVO);
        p.setType(type);
        p.setCreated_at(LocalDateTime.now());
        p.setCollection(collection);
        p.setSeller(seller);
        p.setImageUrls(new java.util.ArrayList<>(List.of("https://example.invalid/products/" + UUID.randomUUID() + ".webp")));
        return products.saveAndFlush(p);
    }

    OrderItem purchase(User buyer, Product p, int quantity, StateOrder state) {
        Order o = new Order();
        o.setUser(buyer);
        o.setRequestKey(UUID.randomUUID());
        o.setOrderDate(LocalDateTime.now());
        o.setState(state);
        o.setPaymentMethod(PaymentMethod.TRANSFERENCIA);
        o.setSubtotal(new BigDecimal("25.00"));
        o.setTotalDiscount(BigDecimal.ZERO);
        o.setTotal(new BigDecimal("25.00"));
        o = orders.saveAndFlush(o);
        OrderItem item = new OrderItem();
        item.setOrder(o);
        item.setProduct(p);
        item.setSeller(seller);
        item.setQuantity(quantity);
        item.setPrice(p.getPrice());
        return orderItems.saveAndFlush(item);
    }

    String purchaseBody(OrderItem item, String section, int page, int slot) {
        return "{\"orderItemId\":%d,\"section\":\"%s\",\"pageNumber\":%d,\"slotIndex\":%d}"
                .formatted(item.getId(), section, page, slot);
    }

    long addPurchase(OrderItem item, String section, int page, int slot) throws Exception {
        var result = mvc.perform(post("/albums/me/cards/purchase").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody(item, section, page, slot)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.origin").value("PURCHASE"))
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    MockMultipartFile picture() {
        return new MockMultipartFile("image", "physical.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test
    void pendingCheckoutOrderIsImmediatelyEligibleWithoutExternalPayment() throws Exception {
        Product p = product("Cromo Cars", ProductType.CARTA);
        OrderItem item = purchase(owner, p, 3, StateOrder.PENDIENTE);
        mvc.perform(get("/albums/me/purchases").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderItemId").value(item.getId()))
                .andExpect(jsonPath("$[0].availableQuantity").value(3))
                .andExpect(jsonPath("$[0].name").value("Cromo Cars"));
        addPurchase(item, "MARVEL", 1, 4); // Theme does not limit purchase placement.
        mvc.perform(get("/albums/me/purchases").header("Authorization", token))
                .andExpect(jsonPath("$[0].availableQuantity").value(2))
                .andExpect(jsonPath("$[0].usedQuantity").value(1));
    }

    @Test
    void purchaseQuantitiesPreventOverdraftAndRemovingReleasesOneCopy() throws Exception {
        OrderItem item = purchase(owner, product("Repeated", ProductType.CARTA), 2, StateOrder.PENDIENTE);
        long first = addPurchase(item, "CARS", 1, 0);
        addPurchase(item, "CARS", 1, 1);
        mvc.perform(post("/albums/me/cards/purchase").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody(item, "NBA", 1, 0)))
                .andExpect(status().isConflict());
        assertThat(albumCards.countByOrderItem_Id(item.getId())).isEqualTo(2);
        mvc.perform(delete("/albums/me/cards/{id}", first).header("Authorization", token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/albums/me/purchases").header("Authorization", token))
                .andExpect(jsonPath("$[0].availableQuantity").value(1));
        addPurchase(item, "NBA", 1, 0);
        assertThat(albumCards.countByOrderItem_Id(item.getId())).isEqualTo(2);
    }

    @Test
    void importedCardRetainsSnapshotAfterPublicationChanges() throws Exception {
        Product p = product("Original name", ProductType.CARTA);
        String firstImage = p.getImageUrls().getFirst();
        OrderItem item = purchase(owner, p, 1, StateOrder.PENDIENTE);
        long cardId = addPurchase(item, "MY_LITTLE_PONY", 1, 8);
        p.setName("Renamed after purchase");
        p.getImageUrls().clear();
        p.getImageUrls().add("https://example.invalid/changed.webp");
        products.flush();
        AlbumCard saved = albumCards.findById(cardId).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Original name");
        assertThat(saved.getImageUrl()).isEqualTo(firstImage);
        assertThat(saved.getOrderItem().getId()).isEqualTo(item.getId());
    }

    @Test
    void foreignPurchasesCannotBeViewedOrImported() throws Exception {
        User other = user("other");
        OrderItem foreign = purchase(other, product("Not mine", ProductType.CARTA), 1, StateOrder.PENDIENTE);
        mvc.perform(get("/albums/me/purchases").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/albums/me/cards/purchase").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody(foreign, "NBA", 1, 0)))
                .andExpect(status().isNotFound());
        assertThat(albumCards.count()).isZero();
    }

    @Test
    void cancelledOrdersAndPacksAreNotEligibleButPaidCardsAre() throws Exception {
        purchase(owner, product("Cancelled", ProductType.CARTA), 1, StateOrder.CANCELADO);
        OrderItem pack = purchase(owner, product("Pack", ProductType.SOBRE), 1, StateOrder.PENDIENTE);
        OrderItem valid = purchase(owner, product("Paid", ProductType.CARTA), 1, StateOrder.PAGO);
        mvc.perform(get("/albums/me/purchases").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderItemId").value(valid.getId()));
        mvc.perform(post("/albums/me/cards/purchase").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody(pack, "NBA", 1, 0)))
                .andExpect(status().isConflict());
    }

    @Test
    void manualRegistrationUsesUploadedPhotoAndDoesNotConsumePurchaseQuota() throws Exception {
        OrderItem purchased = purchase(owner, product("Same as physical", ProductType.CARTA), 1, StateOrder.PENDIENTE);
        when(images.uploadAlbumImage(eq(owner.getId()), any()))
                .thenReturn("https://example.invalid/storage/v1/object/public/cards/albums/manual.webp");
        mvc.perform(multipart("/albums/me/cards/manual")
                        .file(picture())
                        .param("section", "CARS").param("pageNumber", "1").param("slotIndex", "2")
                        .param("name", "  Mi carta física  ")
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin").value("MANUAL"))
                .andExpect(jsonPath("$.name").value("Mi carta física"))
                .andExpect(jsonPath("$.slotIndex").value(2));
        assertThat(albumCards.countByOrderItem_Id(purchased.getId())).isZero();
        addPurchase(purchased, "CARS", 1, 3);
        verify(images).uploadAlbumImage(eq(owner.getId()), any());
    }

    @Test
    void occupiedSlotAndInvalidNameNeverStartUpload() throws Exception {
        OrderItem item = purchase(owner, product("Owned card", ProductType.CARTA), 1, StateOrder.PENDIENTE);
        addPurchase(item, "NBA", 1, 0);
        mvc.perform(multipart("/albums/me/cards/manual")
                        .file(picture()).param("section", "NBA").param("pageNumber", "1")
                        .param("slotIndex", "0").param("name", "Manual")
                        .header("Authorization", token))
                .andExpect(status().isConflict());
        mvc.perform(multipart("/albums/me/cards/manual")
                        .file(picture()).param("section", "NBA").param("pageNumber", "1")
                        .param("slotIndex", "1").param("name", "  ")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
        verify(images, never()).uploadAlbumImage(anyLong(), any());
    }

    @Test
    void invalidPhotoFailsWithoutPersistingCard() throws Exception {
        when(images.uploadAlbumImage(eq(owner.getId()), any()))
                .thenThrow(new InvalidImageException("Formato de imagen no permitido"));
        mvc.perform(multipart("/albums/me/cards/manual")
                        .file(picture()).param("section", "NBA").param("pageNumber", "1")
                        .param("slotIndex", "1").param("name", "Physical")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
        assertThat(albumCards.count()).isZero();
    }

    @Test
    void ninthPurchasedCardOpensNextPageAndTenthCanBeRegistered() throws Exception {
        OrderItem item = purchase(owner, product("Multipack singles", ProductType.CARTA), 10, StateOrder.PENDIENTE);
        for (int i = 0; i < 9; i++) addPurchase(item, "NBA", 1, i);
        Album album = albums.findByUserId(owner.getId()).orElseThrow();
        assertThat(albumSections.findByAlbumIdAndCollectionKey(album.getId(), AlbumSectionKey.NBA)
                .orElseThrow().getPageCount()).isEqualTo(2);
        addPurchase(item, "NBA", 2, 0);
        assertThat(albumCards.countByOrderItem_Id(item.getId())).isEqualTo(10);
    }

    @Test
    void checkoutToAlbumIntegrationUsesActualCreatedOrder() throws Exception {
        Product p = product("From checkout", ProductType.CARTA);
        Cart cart = new Cart();
        cart.setUser(owner);
        cart.setCreatedAt(LocalDateTime.now());
        cart = carts.saveAndFlush(cart);
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(p);
        cartItem.setQuantity(2);
        cartItems.saveAndFlush(cartItem);
        String preview = "{\"paymentMethod\":\"TRANSFERENCIA\"}";
        var quote = mvc.perform(post("/orders/preview").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(preview))
                .andExpect(status().isOk()).andReturn();
        Object total = JsonPath.read(quote.getResponse().getContentAsString(), "$.total");
        String checkout = "{\"paymentMethod\":\"TRANSFERENCIA\",\"expectedTotal\":%s,\"requestKey\":\"%s\"}"
                .formatted(total, UUID.randomUUID());
        var result = mvc.perform(post("/orders").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(checkout))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("PENDIENTE"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn();
        long itemId = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.items[0].id")).longValue();
        mvc.perform(get("/albums/me/purchases").header("Authorization", token))
                .andExpect(jsonPath("$[0].orderItemId").value(itemId))
                .andExpect(jsonPath("$[0].availableQuantity").value(2));
        mvc.perform(post("/albums/me/cards/purchase").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\":%d,\"section\":\"MARVEL\",\"pageNumber\":1,\"slotIndex\":5}".formatted(itemId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin").value("PURCHASE"));
        assertThat(p.getStock()).isEqualTo(18);
        assertThat(cartItems.findByCart_Id(cart.getId())).isEmpty();
    }

    @Test
    void unauthenticatedAddAndPurchasesAreRejected() throws Exception {
        mvc.perform(get("/albums/me/purchases")).andExpect(status().isUnauthorized());
        mvc.perform(post("/albums/me/cards/purchase").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\":1,\"section\":\"NBA\",\"pageNumber\":1,\"slotIndex\":0}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/albums/me/cards/manual")
                        .file(picture()).param("section", "NBA").param("pageNumber", "1")
                        .param("slotIndex", "0").param("name", "Manual"))
                .andExpect(status().isUnauthorized());
    }
}
