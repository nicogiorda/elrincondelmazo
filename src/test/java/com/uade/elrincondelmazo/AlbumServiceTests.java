package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.entity.*;
import com.uade.elrincondelmazo.enums.*;
import com.uade.elrincondelmazo.repository.*;
import com.uade.elrincondelmazo.service.impl.AlbumServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlbumServiceTests {
    @Test
    void firstReadCreatesPrivateAlbumAndFourSectionsOnce() {
        AlbumRepository albums = mock(AlbumRepository.class);
        AlbumSectionRepository sections = mock(AlbumSectionRepository.class);
        AlbumCardRepository cards = mock(AlbumCardRepository.class);
        UserRepository users = mock(UserRepository.class);
        User user = new User();
        user.setId(7L);
        when(users.lockById(7L)).thenReturn(Optional.of(user));
        when(albums.findByUserId(7L)).thenReturn(Optional.empty());
        when(albums.save(any(Album.class))).thenAnswer(invocation -> {
            Album a = invocation.getArgument(0);
            a.setId(10L);
            return a;
        });
        when(sections.findByAlbumIdOrderByIdAsc(10L)).thenReturn(new ArrayList<>());
        final long[] sequence = {20L};
        when(sections.save(any(AlbumSection.class))).thenAnswer(invocation -> {
            AlbumSection s = invocation.getArgument(0);
            s.setId(sequence[0]++);
            return s;
        });
        when(cards.findBySectionIdInOrderByPageNumberAscSlotIndexAsc(anyList())).thenReturn(List.of());

        var result = new AlbumServiceImpl(albums, sections, cards, users, mock(com.uade.elrincondelmazo.service.ImageStorageService.class)).getMine(7L);

        assertThat(result.userId()).isEqualTo(7L);
        assertThat(result.visibility()).isEqualTo(AlbumVisibility.PRIVATE);
        assertThat(result.sections()).extracting(s -> s.key())
            .containsExactly(AlbumSectionKey.NBA, AlbumSectionKey.MY_LITTLE_PONY,
                AlbumSectionKey.CARS, AlbumSectionKey.MARVEL);
        assertThat(result.sections()).allSatisfy(s -> {
            assertThat(s.pageCount()).isEqualTo(1);
            assertThat(s.cards()).isEmpty();
        });
        verify(sections, times(4)).save(any(AlbumSection.class));
    }

    @Test
    void existingAlbumDoesNotDuplicateSections() {
        AlbumRepository albums = mock(AlbumRepository.class);
        AlbumSectionRepository sections = mock(AlbumSectionRepository.class);
        AlbumCardRepository cards = mock(AlbumCardRepository.class);
        UserRepository users = mock(UserRepository.class);
        User user = new User();
        user.setId(8L);
        Album album = new Album();
        album.setId(11L);
        album.setUser(user);
        when(users.lockById(8L)).thenReturn(Optional.of(user));
        when(albums.findByUserId(8L)).thenReturn(Optional.of(album));
        List<AlbumSection> existing = new ArrayList<>();
        long id = 30L;
        for (AlbumSectionKey key : AlbumSectionKey.values()) {
            AlbumSection section = new AlbumSection();
            section.setId(id++);
            section.setAlbum(album);
            section.setCollectionKey(key);
            existing.add(section);
        }
        when(sections.findByAlbumIdOrderByIdAsc(11L)).thenReturn(existing);
        when(cards.findBySectionIdInOrderByPageNumberAscSlotIndexAsc(anyList())).thenReturn(List.of());
        var result = new AlbumServiceImpl(albums, sections, cards, users, mock(com.uade.elrincondelmazo.service.ImageStorageService.class)).getMine(8L);
        assertThat(result.sections()).hasSize(4);
        verify(albums, never()).save(any());
        verify(sections, never()).save(any());
    }
}
