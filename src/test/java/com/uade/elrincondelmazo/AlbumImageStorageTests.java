package com.uade.elrincondelmazo;

import com.uade.elrincondelmazo.exception.InvalidImageException;
import com.uade.elrincondelmazo.service.impl.ImageStorageServiceImpl;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;
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
    void validPngIsConvertedUploadedUnderDedicatedAlbumPrefixAndCanBeDeleted() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> uploadPath = new AtomicReference<>();
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> uploadedType = new AtomicReference<>();
        AtomicReference<String> uploadApiKey = new AtomicReference<>();
        AtomicReference<String> uploadAuthorization = new AtomicReference<>();
        AtomicReference<byte[]> uploadedBytes = new AtomicReference<>();
        server.createContext("/storage/v1/object/cards/", exchange -> {
            method.set(exchange.getRequestMethod());
            if ("POST".equals(method.get())) {
                uploadPath.set(exchange.getRequestURI().getPath());
                uploadedType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
                uploadApiKey.set(exchange.getRequestHeaders().getFirst("apikey"));
                uploadAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                uploadedBytes.set(exchange.getRequestBody().readAllBytes());
            } else {
                uploadPath.set(exchange.getRequestURI().getPath());
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "supabaseUrl",
                    "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(service, "bucket", "cards");
            ReflectionTestUtils.setField(service, "secretKey", "ci-placeholder");

            BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            assertThat(ImageIO.write(image, "png", bytes)).isTrue();

            String url = service.uploadAlbumImage(77L,
                    new MockMultipartFile("image", "sample.png", "image/png", bytes.toByteArray()));
            assertThat(url).contains("/storage/v1/object/public/cards/albums/77/");
            assertThat(method.get()).isEqualTo("POST");
            assertThat(uploadPath.get()).matches(".*/storage/v1/object/cards/albums/77/[0-9a-f-]{36}\\.webp");
            assertThat(uploadedType.get()).startsWith("image/webp");
            assertThat(uploadApiKey.get()).isEqualTo("ci-placeholder");
            assertThat(uploadAuthorization.get()).isEqualTo("Bearer ci-placeholder");
            assertThat(uploadedBytes.get()).isNotEmpty();

            service.deleteAlbumImage(77L, url);
            assertThat(method.get()).isEqualTo("DELETE");
            assertThat(uploadPath.get()).matches(".*/storage/v1/object/cards/albums/77/[0-9a-f-]{36}\\.webp");
        } finally {
            server.stop(0);
        }
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
