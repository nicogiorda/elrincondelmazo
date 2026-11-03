package com.uade.elrincondelmazo.service.impl;

import com.uade.elrincondelmazo.entity.*;
import com.uade.elrincondelmazo.entity.dto.album.AlbumResponse;
import com.uade.elrincondelmazo.entity.dto.album.MoveAlbumCardRequest;
import com.uade.elrincondelmazo.enums.AlbumSectionKey;
import com.uade.elrincondelmazo.repository.*;
import com.uade.elrincondelmazo.service.AlbumService;
import com.uade.elrincondelmazo.service.ImageStorageService;
import com.uade.elrincondelmazo.enums.AlbumCardOrigin;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlbumServiceImpl implements AlbumService {
    private final AlbumRepository albums;
    private final AlbumSectionRepository sections;
    private final AlbumCardRepository cards;
    private final UserRepository users;
    private final ImageStorageService imageStorage;

    public AlbumServiceImpl(AlbumRepository albums, AlbumSectionRepository sections,
                            AlbumCardRepository cards, UserRepository users, ImageStorageService imageStorage) {
        this.albums = albums;
        this.sections = sections;
        this.cards = cards;
        this.users = users;
        this.imageStorage = imageStorage;
    }

    @Override
    @Transactional
    public AlbumResponse getMine(Long userId) {
        User user = lockOwner(userId);
        Album album = getOrCreateAlbum(user);
        List<AlbumSection> stored = sections.findByAlbumIdOrderByIdAsc(album.getId());
        Set<AlbumSectionKey> present = stored.stream()
                .map(AlbumSection::getCollectionKey).collect(Collectors.toSet());
        for (AlbumSectionKey key : AlbumSectionKey.values()) {
            if (!present.contains(key)) {
                AlbumSection section = new AlbumSection();
                section.setAlbum(album);
                section.setCollectionKey(key);
                stored.add(sections.save(section));
            }
        }

        List<Long> ids = stored.stream().map(AlbumSection::getId).toList();
        Map<Long, List<AlbumResponse.CardResponse>> bySection = new HashMap<>();
        if (!ids.isEmpty()) {
            for (AlbumCard card : cards.findBySectionIdInOrderByPageNumberAscSlotIndexAsc(ids)) {
                bySection.computeIfAbsent(card.getSection().getId(), ignored -> new ArrayList<>())
                        .add(toResponse(card));
            }
        }

        Map<AlbumSectionKey, AlbumSection> indexed = stored.stream()
                .collect(Collectors.toMap(AlbumSection::getCollectionKey, section -> section));
        List<AlbumResponse.SectionResponse> result = Arrays.stream(AlbumSectionKey.values())
                .map(key -> {
                    AlbumSection section = indexed.get(key);
                    return new AlbumResponse.SectionResponse(section.getId(), key,
                            section.getPageCount(), bySection.getOrDefault(section.getId(), List.of()));
                }).toList();
        return new AlbumResponse(album.getId(), userId, album.getVisibility(), result);
    }

    @Override
    @Transactional
    public AlbumResponse.CardResponse moveCard(Long userId, Long cardId, MoveAlbumCardRequest request) {
        lockOwner(userId); // Serialize every mutation for this user's album.
        AlbumCard card = ownedCard(userId, cardId);
        Album album = card.getSection().getAlbum();
        AlbumSection target = sections.findByAlbumIdAndCollectionKey(album.getId(), request.section())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sección no encontrada"));

        int page = request.pageNumber();
        int slot = request.slotIndex();
        if (page < 1 || page > target.getPageCount() || slot < 0 || slot > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Posición de álbum inválida");
        }
        if (Objects.equals(card.getSection().getId(), target.getId())
                && card.getPageNumber() == page && card.getSlotIndex() == slot) {
            return toResponse(card);
        }

        Optional<AlbumCard> occupant = cards.findBySectionIdAndPageNumberAndSlotIndex(target.getId(), page, slot);
        if (occupant.isPresent()) {
            if (!request.replaceExisting()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La posición ya está ocupada");
            }
            // Remove the target row before updating the source to respect the unique slot index.
            cards.delete(occupant.get());
            cards.flush();
            cleanupManualImageAfterCommit(userId, occupant.get());
        }

        card.setSection(target);
        card.setPageNumber(page);
        card.setSlotIndex(slot);
        cards.saveAndFlush(card);
        openNextPageWhenFull(target);
        return toResponse(card);
    }

    @Override
    @Transactional
    public void removeCard(Long userId, Long cardId) {
        lockOwner(userId);
        AlbumCard card = ownedCard(userId, cardId);
        cards.delete(card);
        cleanupManualImageAfterCommit(userId, card);
        // Removing a card intentionally never removes already unlocked pages.
    }

    private void cleanupManualImageAfterCommit(Long userId, AlbumCard card) {
        if (card.getOrigin() != AlbumCardOrigin.MANUAL) return;
        String url = card.getImageUrl();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                imageStorage.deleteAlbumImage(userId, url);
            }
        });
    }

    private void openNextPageWhenFull(AlbumSection section) {
        if (cards.countBySectionIdAndPageNumber(section.getId(), section.getPageCount()) == 9) {
            section.setPageCount(section.getPageCount() + 1);
            sections.save(section);
        }
    }

    private User lockOwner(Long userId) {
        return users.lockById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario inexistente"));
    }

    private Album getOrCreateAlbum(User user) {
        return albums.findByUserId(user.getId()).orElseGet(() -> {
            Album created = new Album();
            created.setUser(user);
            return albums.save(created);
        });
    }

    private AlbumCard ownedCard(Long userId, Long cardId) {
        AlbumCard card = cards.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carta no encontrada"));
        if (!Objects.equals(card.getSection().getAlbum().getUser().getId(), userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Carta no encontrada");
        }
        return card;
    }

    private AlbumResponse.CardResponse toResponse(AlbumCard card) {
        return new AlbumResponse.CardResponse(card.getId(), card.getPageNumber(), card.getSlotIndex(),
                card.getName(), card.getImageUrl(), card.getOrigin());
    }
}
