package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.entity.*;
import com.uade.elrincondelmazo.enums.*;
import com.uade.elrincondelmazo.repository.*;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("audit")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class CatalogPerformanceIntegrationTests {
    @Autowired EntityManager em;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired CollectionRepository collections;
    @Autowired MockMvc mvc;
    @Test void measuresColdAndWarmPagesWithImagesAndBoundedSql() throws Exception {
        String prefix="QA_PERF_"+UUID.randomUUID();
        User seller=users.save(User.builder().email(prefix+"@example.invalid")
                .passwordHash("fixture").firstName("QA").lastName("Perf").role(Role.USUARIO)
                .createdAt(LocalDateTime.now()).active(true).build());
        var collection=new com.uade.elrincondelmazo.entity.Collection();collection.setName(prefix);collection=collections.save(collection);
        for(int i=0;i<101;i++) {
            Product p=new Product();p.setName(prefix+i);p.setDescription("QA");p.setPrice(BigDecimal.TEN);
            p.setStock(10);p.setStatus(ProductStatus.ACTIVO);p.setType(ProductType.CARTA);
            p.setSeller(seller);p.setCollection(collection);p.setCreated_at(LocalDateTime.now());
            p.setImageUrls(List.of("https://example.invalid/qa.png"));products.save(p);
        }
        em.flush();em.clear();
        var statistics=em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        for(int size:new int[]{4,9,100})for(int sample=1;sample<=3;sample++) {
            em.clear();statistics.clear();long start=System.nanoTime();
            var response=mvc.perform(get("/products").param("search",prefix).param("size",Integer.toString(size)))
                    .andExpect(status().isOk()).andReturn().getResponse();
            double milliseconds=(System.nanoTime()-start)/1_000_000.0;
            System.out.printf(java.util.Locale.ROOT,"CATALOG_PERF size=%d sample=%d backend_ms=%.2f sql=%d bytes=%d%n",
                    size,sample,milliseconds,statistics.getPrepareStatementCount(),response.getContentAsByteArray().length);
            // 1 page + 1 count + at most 2 batched image queries; no per-product N+1.
            assertThat(statistics.getPrepareStatementCount()).isBetween(1L,4L);
        }
    }
}
