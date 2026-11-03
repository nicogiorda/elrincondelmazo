package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.controllers.config.JwtService;
import com.uade.elrincondelmazo.entity.*;
import com.uade.elrincondelmazo.entity.dto.*;
import com.uade.elrincondelmazo.enums.*;
import com.uade.elrincondelmazo.exception.InvalidCartException;
import com.uade.elrincondelmazo.repository.*;
import com.uade.elrincondelmazo.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("audit")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class CheckoutSafetyIntegrationTests {
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired CollectionRepository collections;
    @Autowired CartRepository carts;
    @MockitoSpyBean CartItemRepository cartItems;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository orderItems;
    @Autowired OrderPromotionRepository orderPromotions;
    @Autowired PromotionRepository promotions;
    @Autowired OrderService checkout;
    @Autowired CartService cartService;
    @Autowired PlatformTransactionManager manager;
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    User seller, buyer, other;
    Product product;
    com.uade.elrincondelmazo.entity.Collection collection;
    TransactionTemplate tx;
    Long promotionId;

    @BeforeEach void fixtures() {
        tx=new TransactionTemplate(manager);
        tx.executeWithoutResult(status -> {
            seller=user(); buyer=user(); other=user();
            collection=new com.uade.elrincondelmazo.entity.Collection(); collection.setName("QA_"+UUID.randomUUID());
            collection=collections.saveAndFlush(collection);
            product=new Product(); product.setName("QA product"); product.setDescription("QA");
            product.setPrice(new BigDecimal("100.00")); product.setStock(2); product.setStatus(ProductStatus.ACTIVO);
            product.setType(ProductType.CARTA); product.setSeller(seller); product.setCollection(collection);
            product.setCreated_at(LocalDateTime.now()); product.setImageUrls(List.of("https://example.invalid/qa.png"));
            product=products.saveAndFlush(product);
        });
        add(buyer,1);
    }
    private User user() {
        String name="qa_"+UUID.randomUUID();
        return users.saveAndFlush(User.builder().email(name+"@example.invalid")
                .firstName("QA").lastName("User").role(Role.USUARIO).active(true)
                .passwordHash("fixture-not-a-password").createdAt(LocalDateTime.now()).build());
    }
    private void add(User u,int qty) {
        AddCartItemRequest r=new AddCartItemRequest(); r.setProductId(product.getId());r.setQuantity(qty);
        cartService.addItem(u.getId(),r);
    }
    private CheckoutRequest request(PaymentMethod method, String total) {
        CheckoutRequest r=new CheckoutRequest();r.setPaymentMethod(method);r.setExpectedTotal(new BigDecimal(total));r.setRequestKey(UUID.randomUUID());return r;
    }
    private int stock() { return products.findById(product.getId()).orElseThrow().getStock(); }
    private void paymentPromotion() {
        tx.executeWithoutResult(status -> {
            Promotion p=new Promotion();p.setName("QA transfer");p.setType(PromotionType.METODO_PAGO);
            p.setPaymentMethod(PaymentMethod.TRANSFERENCIA);p.setDiscountPercentage(BigDecimal.TEN);
            p.setStartDate(LocalDateTime.now().minusDays(1));p.setActive(true);p.setStackable(false);
            promotionId=promotions.saveAndFlush(p).getId();
        });
    }
    @AfterEach void cleanup() {
        reset(cartItems);
        tx.executeWithoutResult(status -> {
            for(User u:List.of(buyer,other,seller)) {
                for(Order o:orders.findByUserId(u.getId())) {
                    orderPromotions.deleteAll(orderPromotions.findByOrderId(o.getId()));
                    orderItems.deleteAll(orderItems.findByOrderId(o.getId()));orders.delete(o);
                }
                carts.findByUserId(u.getId()).ifPresent(c->{cartItems.deleteAll(cartItems.findByCart_Id(c.getId()));carts.delete(c);});
            }
            if(promotionId!=null)promotions.deleteById(promotionId);
            products.deleteById(product.getId());collections.deleteById(collection.getId());
            users.deleteAllById(List.of(buyer.getId(),other.getId(),seller.getId()));
        });
    }
    @Test void previewRecalculatesPaymentDiscountWithoutWriting() throws Exception {
        paymentPromotion();
        var transfer=checkout.preview(buyer.getId(),request(PaymentMethod.TRANSFERENCIA,"90"));
        var card=checkout.preview(buyer.getId(),request(PaymentMethod.TARJETA_DE_CREDITO,"100"));
        assertThat(transfer.getTotal()).isEqualByComparingTo("90");
        assertThat(transfer.getTotalDiscount()).isEqualByComparingTo("10");
        assertThat(card.getTotal()).isEqualByComparingTo("100");
        assertThat(stock()).isEqualTo(2);
        assertThat(orders.findByUserId(buyer.getId())).isEmpty();
        assertThat(cartService.getOrCreateCart(buyer.getId()).getItems()).hasSize(1);
        mvc.perform(post("/orders/preview").header("Authorization","Bearer "+jwt.generateToken(buyer))
                .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\":\"TRANSFERENCIA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(90));
    }
    @Test void successKeepsHistoricalPricesClearsCartAndReplaysSameOrder() throws Exception {
        var r=request(PaymentMethod.MERCADO_PAGO,"100");
        var order=checkout.checkout(buyer.getId(),r);
        assertThat(stock()).isEqualTo(1);
        assertThat(cartService.getOrCreateCart(buyer.getId()).getItems()).isEmpty();
        assertThat(checkout.checkout(buyer.getId(),r).getId()).isEqualTo(order.getId());
        assertThat(orders.findByUserId(buyer.getId())).hasSize(1);
        tx.executeWithoutResult(status -> {var p=products.findById(product.getId()).orElseThrow();p.setPrice(new BigDecimal("200"));});
        assertThat(checkout.getOrderById(buyer.getId(),order.getId()).getItems().getFirst().getPrice()).isEqualByComparingTo("100");
        mvc.perform(get("/orders").header("Authorization","Bearer "+jwt.generateToken(buyer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(order.getId()));
    }
    @Test void changedQuoteOrManipulatedPriceCannotCreateAnOrder() {
        assertThatThrownBy(()->checkout.checkout(buyer.getId(),request(PaymentMethod.MERCADO_PAGO,"1")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        assertThat(stock()).isEqualTo(2);assertThat(orders.findByUserId(buyer.getId())).isEmpty();
        assertThat(cartService.getOrCreateCart(buyer.getId()).getItems()).hasSize(1);
    }
    @Test void cartValidatesStockOwnProductsAndInactiveProductsAndMutatesQuantity() {
        assertThatThrownBy(()->add(seller,1)).isInstanceOf(InvalidCartException.class);
        assertThatThrownBy(()->add(buyer,3)).isInstanceOf(InvalidCartException.class);
        var item=cartService.getOrCreateCart(buyer.getId()).getItems().getFirst();
        var quantity=new UpdateCartItemQuantityRequest();quantity.setQuantity(2);
        assertThat(cartService.updateItemQuantity(buyer.getId(),item.getId(),quantity).getItems().getFirst().getQuantity()).isEqualTo(2);
        assertThat(cartService.removeItem(buyer.getId(),item.getId()).getItems()).isEmpty();
        tx.executeWithoutResult(status->products.findById(product.getId()).orElseThrow().setStatus(ProductStatus.INACTIVO));
        assertThatThrownBy(()->add(buyer,1)).isInstanceOf(InvalidCartException.class);
    }
    @Test void insufficientStockAndOwnProductAreRevalidatedAtCheckout() {
        tx.executeWithoutResult(status->products.findById(product.getId()).orElseThrow().setStock(0));
        assertThatThrownBy(()->checkout.checkout(buyer.getId(),request(PaymentMethod.MERCADO_PAGO,"100"))).isInstanceOf(InvalidCartException.class);
        tx.executeWithoutResult(status->{var p=products.findById(product.getId()).orElseThrow();p.setStock(2);p.setSeller(buyer);});
        assertThatThrownBy(()->checkout.checkout(buyer.getId(),request(PaymentMethod.MERCADO_PAGO,"100"))).isInstanceOf(InvalidCartException.class);
    }
    @Test void failureAfterStockUpdateRollsBackEverything() {
        doThrow(new IllegalStateException("QA rollback after stock write")).when(cartItems).deleteAll(any(Iterable.class));
        assertThatThrownBy(()->checkout.checkout(buyer.getId(),request(PaymentMethod.MERCADO_PAGO,"100"))).hasRootCauseInstanceOf(IllegalStateException.class);
        reset(cartItems);
        assertThat(stock()).isEqualTo(2);assertThat(orders.findByUserId(buyer.getId())).isEmpty();
        assertThat(cartService.getOrCreateCart(buyer.getId()).getItems()).hasSize(1);
    }
    @Test void twoBuyersCannotPurchaseTheLastUnit() throws Exception {
        tx.executeWithoutResult(status->products.findById(product.getId()).orElseThrow().setStock(1));
        add(other,1);
        CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
        try(ExecutorService executor=Executors.newFixedThreadPool(2)) {
            var futures=List.of(buyer,other).stream().map(u->executor.submit(()->{
                ready.countDown();start.await();
                try {return checkout.checkout(u.getId(),request(PaymentMethod.MERCADO_PAGO,"100")).getId();}
                catch(InvalidCartException expected){return null;}
            })).toList();
            assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();start.countDown();
            int successes=0;for(var f:futures)if(f.get(20,TimeUnit.SECONDS)!=null)successes++;
            assertThat(successes).isEqualTo(1);
        }
        assertThat(stock()).isZero();
        assertThat(products.findById(product.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.AGOTADO);
        assertThat(orders.findByUserId(buyer.getId()).size()+orders.findByUserId(other.getId()).size()).isEqualTo(1);
    }
}
