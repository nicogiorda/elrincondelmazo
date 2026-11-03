package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.entity.*;
import com.uade.elrincondelmazo.enums.*;
import com.uade.elrincondelmazo.repository.*;
import com.uade.elrincondelmazo.service.ImageStorageService;
import com.uade.elrincondelmazo.service.impl.AlbumServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Optional;

import static org.mockito.Mockito.*;

class AlbumManualCleanupTests {
    @Test
    void deletingManualCardCleansPhotoOnlyAfterCommit() {
        AlbumRepository albums = mock(AlbumRepository.class);
        AlbumSectionRepository sections = mock(AlbumSectionRepository.class);
        AlbumCardRepository cards = mock(AlbumCardRepository.class);
        UserRepository users = mock(UserRepository.class);
        ImageStorageService images = mock(ImageStorageService.class);

        User user = new User();
        user.setId(77L);
        Album album = new Album();
        album.setUser(user);
        AlbumSection section = new AlbumSection();
        section.setAlbum(album);
        AlbumCard manual = new AlbumCard();
        manual.setId(10L);
        manual.setSection(section);
        manual.setOrigin(AlbumCardOrigin.MANUAL);
        manual.setImageUrl("https://example.invalid/albums/77/image.webp");

        when(users.lockById(77L)).thenReturn(Optional.of(user));
        when(cards.findById(10L)).thenReturn(Optional.of(manual));

        TransactionSynchronizationManager.initSynchronization();
        try {
            new AlbumServiceImpl(albums, sections, cards, users, images).removeCard(77L, 10L);
            verify(cards).delete(manual);
            verifyNoInteractions(images);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            verify(images).deleteAlbumImage(77L, manual.getImageUrl());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletingPurchasedCardDoesNotRemoveProductPhoto() {
        AlbumRepository albums = mock(AlbumRepository.class);
        AlbumSectionRepository sections = mock(AlbumSectionRepository.class);
        AlbumCardRepository cards = mock(AlbumCardRepository.class);
        UserRepository users = mock(UserRepository.class);
        ImageStorageService images = mock(ImageStorageService.class);
        User user = new User();
        user.setId(77L);
        Album album = new Album();
        album.setUser(user);
        AlbumSection section = new AlbumSection();
        section.setAlbum(album);
        AlbumCard purchase = new AlbumCard();
        purchase.setId(11L);
        purchase.setSection(section);
        purchase.setOrigin(AlbumCardOrigin.PURCHASE);
        when(users.lockById(77L)).thenReturn(Optional.of(user));
        when(cards.findById(11L)).thenReturn(Optional.of(purchase));
        new AlbumServiceImpl(albums, sections, cards, users, images).removeCard(77L, 11L);
        verifyNoInteractions(images);
    }
}
