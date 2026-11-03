package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.exception.InvalidImageException;
import com.uade.elrincondelmazo.service.impl.ImageStorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlbumImageStorageTests {
    private final ImageStorageServiceImpl service = new ImageStorageServiceImpl();

    @Test
    void rejectsMissingAndEmptyManualImagesBeforeStorage() {
        assertThatThrownBy(() -> service.uploadAlbumImage(1L, null))
                .isInstanceOf(InvalidImageException.class);
        assertThatThrownBy(() -> service.uploadAlbumImage(1L,
                new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0])))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void rejectsUnsupportedTypeAndFakePngBytes() {
        assertThatThrownBy(() -> service.uploadAlbumImage(1L,
                new MockMultipartFile("image", "bad.txt", "text/plain", "hi".getBytes())))
                .isInstanceOf(InvalidImageException.class);
        assertThatThrownBy(() -> service.uploadAlbumImage(1L,
                new MockMultipartFile("image", "fake.png", "image/png", "not an image".getBytes())))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void rejectsPhotosLargerThanTenMegabytes() {
        assertThatThrownBy(() -> service.uploadAlbumImage(1L,
                new MockMultipartFile("image", "large.png", "image/png",
                        new byte[10 * 1024 * 1024 + 1])))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void deletionRefusesExternalOrAnotherUsersUrls() {
        ReflectionTestUtils.setField(service, "supabaseUrl", "https://example.invalid");
        ReflectionTestUtils.setField(service, "bucket", "cards");
        assertThatThrownBy(() -> service.deleteAlbumImage(123L,
                "https://other.example/albums/123/something.webp"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deleteAlbumImage(123L,
                "https://example.invalid/storage/v1/object/public/cards/albums/124/00000000-0000-0000-0000-000000000000.webp"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deleteAlbumImage(123L,
                "https://example.invalid/storage/v1/object/public/cards/albums/123/../products/123/not-safe.webp"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
