package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.controllers.config.JwtService;
import com.uade.elrincondelmazo.entity.*;
import com.uade.elrincondelmazo.enums.*;
import com.uade.elrincondelmazo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("audit")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
class AlbumPrivacyIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired AlbumRepository albums;
    @Autowired AlbumCardRepository cards;
    @Autowired AlbumSectionRepository sections;

    User owner, stranger;
    String ownerToken, strangerToken;

    @BeforeEach
    void setup() throws Exception {
        owner = user("owner");
        stranger = user("stranger");
        ownerToken = auth(owner);
        strangerToken = auth(stranger);
        mvc.perform(get("/albums/me").header("Authorization", ownerToken))
                .andExpect(status().isOk());
    }

    User user(String label) {
        return users.saveAndFlush(User.builder()
                .email("album-privacy-" + label + "-" + UUID.randomUUID() + "@example.invalid")
                .firstName("Album").lastName(label)
                .passwordHash("integration-only")
                .role(Role.USUARIO).active(true).createdAt(LocalDateTime.now()).build());
    }

    String auth(User user) {
        return "Bearer " + jwt.generateToken(user);
    }

    void setPublic() throws Exception {
        mvc.perform(patch("/albums/me/visibility")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));
    }

    @Test
    void defaultPrivateAlbumIsNotVisibleToOtherUserOrGuests() throws Exception {
        mvc.perform(get("/albums/users/{id}", owner.getId()).header("Authorization", strangerToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/albums/users/{id}", owner.getId()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/albums/me").header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
    }

    @Test
    void publicAlbumExposesReadOnlySnapshotToAuthenticatedOtherUser() throws Exception {
        Album album = albums.findByUserId(owner.getId()).orElseThrow();
        AlbumSection section = sections.findByAlbumIdAndCollectionKey(album.getId(), AlbumSectionKey.CARS)
                .orElseThrow();
        AlbumCard card = new AlbumCard();
        card.setSection(section);
        card.setPageNumber(1);
        card.setSlotIndex(4);
        card.setName("Mi carta de Cars");
        card.setImageUrl("https://example.invalid/album/owned.webp");
        card.setOrigin(AlbumCardOrigin.MANUAL);
        cards.saveAndFlush(card);
        setPublic();
        mvc.perform(get("/albums/users/{id}", owner.getId()).header("Authorization", strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(owner.getId()))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.sections.length()").value(4))
                .andExpect(jsonPath("$.sections[2].cards[0].id").value(card.getId()))
                .andExpect(jsonPath("$.sections[2].cards[0].name").value("Mi carta de Cars"));
        mvc.perform(patch("/albums/me/cards/{id}", card.getId()).header("Authorization", strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"section\":\"CARS\",\"pageNumber\":1,\"slotIndex\":5}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/albums/me/cards/{id}", card.getId())
                        .header("Authorization", strangerToken))
                .andExpect(status().isNotFound());
        assertThat(cards.findById(card.getId())).isPresent();
    }

    @Test
    void returningToPrivateImmediatelyRevokesPublicRead() throws Exception {
        setPublic();
        mvc.perform(get("/albums/users/{id}", owner.getId()).header("Authorization", strangerToken))
                .andExpect(status().isOk());
        mvc.perform(patch("/albums/me/visibility").header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PRIVATE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
        mvc.perform(get("/albums/users/{id}", owner.getId()).header("Authorization", strangerToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/albums/users/{id}", owner.getId()).header("Authorization", ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void unknownUserAndNeverCreatedAlbumAreNotLeaked() throws Exception {
        mvc.perform(get("/albums/users/{id}", 987654321L).header("Authorization", strangerToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/albums/users/{id}", stranger.getId()).header("Authorization", ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void visibilityUpdatesCannotTargetAnotherAccount() throws Exception {
        // Only /me/visibility exists; the current authenticated user owns any update.
        mvc.perform(patch("/albums/me/visibility").header("Authorization", strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(stranger.getId()));
        assertThat(albums.findByUserId(owner.getId()).orElseThrow().getVisibility())
                .isEqualTo(AlbumVisibility.PRIVATE);
    }

    @Test
    void invalidVisibilityAndAnonymousMutationAreRejected() throws Exception {
        mvc.perform(patch("/albums/me/visibility").header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":null}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/albums/me/visibility").header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"VISIBLE_TO_ALL\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/albums/me/visibility")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatedOwnersAreNotPubliclyVisible() throws Exception {
        setPublic();
        owner.setActive(false);
        users.saveAndFlush(owner);
        mvc.perform(get("/albums/users/{id}", owner.getId()).header("Authorization", strangerToken))
                .andExpect(status().isNotFound());
    }
}
