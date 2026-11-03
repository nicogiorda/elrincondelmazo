package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.controllers.PromotionController;
import com.uade.elrincondelmazo.repository.PromotionRepository;
import com.uade.elrincondelmazo.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PromotionPublicTests {
    @Mock PromotionRepository repository;
    @InjectMocks PromotionServiceImpl service;

    @Test
    void returnsAnEmptyArrayWithoutFabricatingPromotions() throws Exception {
        when(repository.findPublicAt(any(LocalDateTime.class))).thenReturn(List.of());
        PromotionController controller = new PromotionController();
        ReflectionTestUtils.setField(controller, "promotionService", service);
        MockMvcBuilders.standaloneSetup(controller).build().perform(get("/promotions/public"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
