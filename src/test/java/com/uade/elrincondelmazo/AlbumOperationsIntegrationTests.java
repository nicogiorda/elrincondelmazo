package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.controllers.config.JwtService;
import com.uade.elrincondelmazo.entity.Album;
import com.uade.elrincondelmazo.entity.AlbumCard;
import com.uade.elrincondelmazo.entity.AlbumSection;
import com.uade.elrincondelmazo.entity.User;
import com.uade.elrincondelmazo.enums.AlbumCardOrigin;
import com.uade.elrincondelmazo.enums.AlbumSectionKey;
import com.uade.elrincondelmazo.enums.AlbumVisibility;
import com.uade.elrincondelmazo.enums.Role;
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
class AlbumOperationsIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired AlbumRepository albums;
    @Autowired AlbumSectionRepository sections;
    @Autowired AlbumCardRepository cards;

    User owner;
    String ownerToken;

    @BeforeEach
    void setUp() {
        owner = user("owner");
        ownerToken = "Bearer " + jwt.generateToken(owner);
    }

    User user(String marker) {
        return users.saveAndFlush(User.builder()
                .email("album_" + marker + "_" + UUID.randomUUID() + "@example.invalid")
                .passwordHash("not-used-in-jwt-test")
                .firstName("Album").lastName("Test")
                .role(Role.USUARIO).active(true).createdAt(LocalDateTime.now()).build());
    }

    void initializeAlbum(User user) throws Exception {
        mvc.perform(get("/albums/me").header("Authorization", "Bearer " + jwt.generateToken(user)))
                .andExpect(status().isOk());
    }

    AlbumSection section(AlbumSectionKey key) {
        Album album = albums.findByUserId(owner.getId()).orElseThrow();
        return sections.findByAlbumIdAndCollectionKey(album.getId(), key).orElseThrow();
    }

    AlbumCard put(AlbumSection section, int page, int slot, String name) {
        AlbumCard card = new AlbumCard();
        card.setSection(section);
        card.setPageNumber(page);
        card.setSlotIndex(slot);
        card.setName(name);
        card.setImageUrl("https://example.invalid/album/" + name + ".webp");
        card.setOrigin(AlbumCardOrigin.MANUAL);
        return cards.saveAndFlush(card);
    }

    String moveBody(String section, int page, int slot, boolean replace) {
        return "{\"section\":\"%s\",\"pageNumber\":%d,\"slotIndex\":%d,\"replaceExisting\":%s}"
                .formatted(section, page, slot, replace);
    }

    @Test
    void createsPrivateFourSectionAlbumExactlyOnceAndRequiresAuthentication() throws Exception {
        mvc.perform(get("/albums/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/albums/me").header("Authorization", ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(owner.getId()))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.sections.length()").value(4))
                .andExpect(jsonPath("$.sections[0].key").value("NBA"))
                .andExpect(jsonPath("$.sections[1].key").value("MY_LITTLE_PONY"))
                .andExpect(jsonPath("$.sections[2].key").value("CARS"))
                .andExpect(jsonPath("$.sections[3].key").value("MARVEL"))
                .andExpect(jsonPath("$.sections[0].pageCount").value(1));
        initializeAlbum(owner);
        assertThat(albums.count()).isEqualTo(1);
        assertThat(sections.findByAlbumIdOrderByIdAsc(section(AlbumSectionKey.NBA).getAlbum().getId()))
                .hasSize(4);
        assertThat(albums.findByUserId(owner.getId()).orElseThrow().getVisibility())
                .isEqualTo(AlbumVisibility.PRIVATE);
    }

    @Test
    void moveBetweenSectionsKeepsCardSnapshotAndLeavesOriginalSpaceFree() throws Exception {
        initializeAlbum(owner);
        AlbumCard source = put(section(AlbumSectionKey.NBA), 1, 2, "same-card");
        mvc.perform(patch("/albums/me/cards/{id}", source.getId()).header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveBody("CARS", 1, 7, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(source.getId()))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.slotIndex").value(7))
                .andExpect(jsonPath("$.name").value("same-card"))
                .andExpect(jsonPath("$.origin").value("MANUAL"));
        cards.flush();
        assertThat(cards.findBySectionIdAndPageNumberAndSlotIndex(
                section(AlbumSectionKey.NBA).getId(), 1, 2)).isEmpty();
        assertThat(cards.findBySectionIdAndPageNumberAndSlotIndex(
                section(AlbumSectionKey.CARS).getId(), 1, 7)).isPresent();
        assertThat(cards.count()).isEqualTo(1);
    }

    @Test
    void occupiedDestinationReturnsConflictWithoutOverwriting() throws Exception {
        initializeAlbum(owner);
        AlbumCard source = put(section(AlbumSectionKey.NBA), 1, 0, "source");
        AlbumCard target = put(section(AlbumSectionKey.CARS), 1, 3, "target");
        mvc.perform(patch("/albums/me/cards/{id}", source.getId()).header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveBody("CARS", 1, 3, false)))
                .andExpect(status().isConflict());
        assertThat(cards.findById(source.getId()).orElseThrow().getSlotIndex()).isZero();
        assertThat(cards.findById(target.getId())).isPresent();
    }

    @Test
    void explicitReplacementDeletesOnlyPreviousOccupant() throws Exception {
        initializeAlbum(owner);
        AlbumCard source = put(section(AlbumSectionKey.NBA), 1, 0, "source");
        AlbumCard target = put(section(AlbumSectionKey.CARS), 1, 3, "target");
        mvc.perform(patch("/albums/me/cards/{id}", source.getId()).header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveBody("CARS", 1, 3, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(source.getId()));
        assertThat(cards.findById(target.getId())).isEmpty();
        assertThat(cards.findById(source.getId()).orElseThrow().getSection().getCollectionKey())
                .isEqualTo(AlbumSectionKey.CARS);
        assertThat(cards.count()).isEqualTo(1);
    }

    @Test
    void ninthCardUnlocksExactlyOneNextPageAndRemovalDoesNotDeletePages() throws Exception {
        initializeAlbum(owner);
        AlbumSection nba = section(AlbumSectionKey.NBA);
        for (int i = 0; i < 8; i++) {
            put(nba, 1, i, "card-" + i);
        }
        AlbumCard ninth = put(section(AlbumSectionKey.MARVEL), 1, 0, "ninth");
        mvc.perform(patch("/albums/me/cards/{id}", ninth.getId()).header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveBody("NBA", 1, 8, false)))
                .andExpect(status().isOk());
        assertThat(section(AlbumSectionKey.NBA).getPageCount()).isEqualTo(2);
        initializeAlbum(owner);
        assertThat(section(AlbumSectionKey.NBA).getPageCount()).isEqualTo(2);

        mvc.perform(patch("/albums/me/cards/{id}", ninth.getId()).header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveBody("NBA", 2, 0, false)))
                .andExpect(status().isOk());
        assertThat(section(AlbumSectionKey.NBA).getPageCount()).isEqualTo(2);
        mvc.perform(delete("/albums/me/cards/{id}", ninth.getId()).header("Authorization", ownerToken))
                .andExpect(status().isNoContent());
        assertThat(cards.findById(ninth.getId())).isEmpty();
        assertThat(section(AlbumSectionKey.NBA).getPageCount()).isEqualTo(2);
    }

    @Test
    void rejectsInvalidSlotAndMissingPage() throws Exception {
        initializeAlbum(owner);
        AlbumCard source = put(section(AlbumSectionKey.NBA), 1, 0, "card");
        mvc.perform(patch("/albums/me/cards/{id}", source.getId()).header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveBody("NBA", 1, 9, false)))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/albums/me/cards/{id}", source.getId()).header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveBody("NBA", 2, 0, false)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void foreignAccountCannotMoveOrDeleteAnotherUsersCard() throws Exception {
        initializeAlbum(owner);
        AlbumCard card = put(section(AlbumSectionKey.MARVEL), 1, 1, "private-card");
        User stranger = user("stranger");
        initializeAlbum(stranger);
        String foreignToken = "Bearer " + jwt.generateToken(stranger);
        mvc.perform(patch("/albums/me/cards/{id}", card.getId()).header("Authorization", foreignToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(moveBody("MARVEL", 1, 2, true)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/albums/me/cards/{id}", card.getId())
                        .header("Authorization", foreignToken))
                .andExpect(status().isNotFound());
        assertThat(cards.findById(card.getId())).isPresent();
    }

    @Test
    void deleteRequiresAuthenticationAndDoesNotAffectOtherCards() throws Exception {
        initializeAlbum(owner);
        AlbumCard toRemove = put(section(AlbumSectionKey.CARS), 1, 0, "to-remove");
        AlbumCard toKeep = put(section(AlbumSectionKey.CARS), 1, 1, "to-keep");
        mvc.perform(delete("/albums/me/cards/{id}", toRemove.getId()))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/albums/me/cards/{id}", toRemove.getId()).header("Authorization", ownerToken))
                .andExpect(status().isNoContent());
        assertThat(cards.findById(toRemove.getId())).isEmpty();
        assertThat(cards.findById(toKeep.getId())).isPresent();
    }
}
