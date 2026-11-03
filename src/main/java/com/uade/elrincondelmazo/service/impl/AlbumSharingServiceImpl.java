package com.uade.elrincondelmazo.service.impl;

import com.uade.elrincondelmazo.entity.*;
import com.uade.elrincondelmazo.entity.dto.album.AlbumResponse;
import com.uade.elrincondelmazo.enums.AlbumSectionKey;
import com.uade.elrincondelmazo.enums.AlbumVisibility;
import com.uade.elrincondelmazo.repository.*;
import com.uade.elrincondelmazo.service.AlbumService;
import com.uade.elrincondelmazo.service.AlbumSharingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlbumSharingServiceImpl implements AlbumSharingService {
    private final AlbumService ownAlbum;
    private final AlbumRepository albums;
    private final AlbumSectionRepository sections;
    private final AlbumCardRepository cards;

    public AlbumSharingServiceImpl(AlbumService ownAlbum, AlbumRepository albums,
                                   AlbumSectionRepository sections, AlbumCardRepository cards) {
        this.ownAlbum = ownAlbum;
        this.albums = albums;
        this.sections = sections;
        this.cards = cards;
    }

    @Override
    @Transactional
    public AlbumResponse setVisibility(Long ownerId, AlbumVisibility visibility) {
        if (visibility == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Visibilidad inválida");
        }
        // The existing getMine path obtains a user lock and creates the album on first use.
        ownAlbum.getMine(ownerId);
        Album album = albums.findByUserId(ownerId).orElseThrow();
        album.setVisibility(visibility);
        albums.save(album);
        return ownAlbum.getMine(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public AlbumResponse viewAlbum(Long viewerId, Long ownerId) {
        // Own albums are available irrespective of visibility; no new account is exposed.
        if (Objects.equals(viewerId, ownerId)) {
            // Existing albums return without creation; the owner route handles lazy creation.
            return viewExistingOwnerAlbum(ownerId);
        }
        Album album = albums.findByUserId(ownerId).orElseThrow(() -> notVisible());
        if (album.getVisibility() != AlbumVisibility.PUBLIC || !Boolean.TRUE.equals(album.getUser().getActive())) {
            throw notVisible();
        }
        return assemble(album);
    }

    private AlbumResponse viewExistingOwnerAlbum(Long ownerId) {
        Album album = albums.findByUserId(ownerId).orElseThrow(() -> notVisible());
        return assemble(album);
    }

    private AlbumResponse assemble(Album album) {
        List<AlbumSection> stored = sections.findByAlbumIdOrderByIdAsc(album.getId());
        Map<Long, List<AlbumResponse.CardResponse>> cardsBySection = new HashMap<>();
        List<Long> ids = stored.stream().map(AlbumSection::getId).toList();
        if (!ids.isEmpty()) {
            for (AlbumCard card : cards.findBySectionIdInOrderByPageNumberAscSlotIndexAsc(ids)) {
                cardsBySection.computeIfAbsent(card.getSection().getId(), ignored -> new ArrayList<>())
                        .add(new AlbumResponse.CardResponse(card.getId(), card.getPageNumber(),
                                card.getSlotIndex(), card.getName(), card.getImageUrl(), card.getOrigin()));
            }
        }
        Map<AlbumSectionKey, AlbumSection> byKey = stored.stream()
                .collect(Collectors.toMap(AlbumSection::getCollectionKey, section -> section));
        List<AlbumResponse.SectionResponse> responses = Arrays.stream(AlbumSectionKey.values())
                .filter(byKey::containsKey)
                .map(key -> {
                    AlbumSection section = byKey.get(key);
                    return new AlbumResponse.SectionResponse(section.getId(), key, section.getPageCount(),
                            cardsBySection.getOrDefault(section.getId(), List.of()));
                }).toList();
        return new AlbumResponse(album.getId(), album.getUser().getId(), album.getVisibility(), responses);
    }

    private ResponseStatusException notVisible() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Álbum no disponible");
    }
}
