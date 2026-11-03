package com.uade.elrincondelmazo.service.impl;
import com.uade.elrincondelmazo.entity.*;
import com.uade.elrincondelmazo.entity.dto.album.AlbumResponse;
import com.uade.elrincondelmazo.enums.AlbumSectionKey;
import com.uade.elrincondelmazo.repository.*;
import com.uade.elrincondelmazo.service.AlbumService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class AlbumServiceImpl implements AlbumService {
    private final AlbumRepository albums;
    private final AlbumSectionRepository sections;
    private final AlbumCardRepository cards;
    private final UserRepository users;
    public AlbumServiceImpl(AlbumRepository albums, AlbumSectionRepository sections, AlbumCardRepository cards, UserRepository users) {
        this.albums = albums;
        this.sections = sections;
        this.cards = cards;
        this.users = users;
    }
    @Override @Transactional
    public AlbumResponse getMine(Long userId) {
        // Lock the owner row to serialize first-time initialization across concurrent requests.
        User user = users.lockById(userId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Usuario inexistente"));
        Album album = albums.findByUserId(userId).orElseGet(() -> {
            Album created = new Album();
            created.setUser(user);
            return albums.save(created);
        });
        List<AlbumSection> stored = sections.findByAlbumIdOrderByIdAsc(album.getId());
        Set<AlbumSectionKey> present = stored.stream().map(AlbumSection::getCollectionKey).collect(Collectors.toSet());
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
                    .add(new AlbumResponse.CardResponse(card.getId(), card.getPageNumber(), card.getSlotIndex(),
                            card.getName(), card.getImageUrl(), card.getOrigin()));
            }
        }
        Map<AlbumSectionKey, AlbumSection> indexed = stored.stream().collect(Collectors.toMap(AlbumSection::getCollectionKey, section -> section));
        List<AlbumResponse.SectionResponse> result = Arrays.stream(AlbumSectionKey.values()).map(key -> {
            AlbumSection section = indexed.get(key);
            return new AlbumResponse.SectionResponse(section.getId(), key, section.getPageCount(),
                    bySection.getOrDefault(section.getId(), List.of()));
        }).toList();
        return new AlbumResponse(album.getId(), userId, album.getVisibility(), result);
    }
}
