package com.uade.elrincondelmazo.service.impl;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import com.uade.elrincondelmazo.exception.ImageStorageException;
import com.uade.elrincondelmazo.exception.InvalidImageException;
import com.uade.elrincondelmazo.service.ImageStorageService;

@Service
public class ImageStorageServiceImpl
        implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ImageStorageServiceImpl.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final int MAX_DIMENSION = 1600;

    private static final float WEBP_QUALITY = 0.82f;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private final RestClient restClient = RestClient.create();

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.secret-key}")
    private String secretKey;

    @Value("${supabase.storage.bucket}")
    private String bucket;

    @Override
    public String uploadProductImage(Long userId, MultipartFile file) {
        return uploadImage("products/" + userId + "/", file);
    }

    @Override
    public String uploadAlbumImage(Long userId, MultipartFile file) {
        return uploadImage("albums/" + userId + "/", file);
    }

    @Override
    public void deleteAlbumImage(Long userId, String imageUrl) {
        // Only ever delete a UUID-named upload under this user's album prefix.
        String prefix = buildPublicUrl("albums/" + userId + "/");
        if (imageUrl == null || !imageUrl.startsWith(prefix)) {
            throw new IllegalArgumentException("La imagen no pertenece al álbum del usuario");
        }
        String filename = imageUrl.substring(prefix.length());
        if (!filename.matches("[0-9a-fA-F-]{36}\\.webp")) {
            throw new IllegalArgumentException("Nombre de imagen no válido");
        }
        String path = "albums/" + userId + "/" + filename;
        try {
            restClient.delete()
                    .uri(supabaseUrl + "/storage/v1/object/" + bucket + "/" + path)
                    .header("apikey", secretKey)
                    .header("Authorization", "Bearer " + secretKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo limpiar la foto del álbum del usuario {}", userId);
        }
    }

    private String uploadImage(String prefix, MultipartFile file) {
        validateFile(file);

        BufferedImage original = readImage(file);

        BufferedImage resized = resizeIfNecessary(original);

        byte[] webpBytes = convertToWebp(resized);

        String path = prefix + UUID.randomUUID() + ".webp";

        uploadToSupabase(
                path,
                webpBytes);

        return buildPublicUrl(path);
    }

    private void validateFile(
            MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidImageException(
                    "La imagen es obligatoria");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidImageException(
                    "La imagen no puede superar los 10 MB");
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !ALLOWED_TYPES.contains(contentType)) {

            throw new InvalidImageException(
                    "Formato de imagen no permitido");
        }
    }

    private BufferedImage readImage(
            MultipartFile file) {

        try {

            BufferedImage image = ImageIO.read(
                    file.getInputStream());

            if (image == null) {
                throw new InvalidImageException(
                        "El archivo no contiene una imagen válida");
            }

            return image;

        } catch (IOException e) {

            throw new InvalidImageException(
                    "No se pudo leer la imagen");
        }
    }

    private BufferedImage resizeIfNecessary(
            BufferedImage source) {

        int width = source.getWidth();
        int height = source.getHeight();

        if (width <= MAX_DIMENSION
                && height <= MAX_DIMENSION) {

            return source;
        }

        double scale = Math.min(
                (double) MAX_DIMENSION / width,
                (double) MAX_DIMENSION / height);

        int newWidth = (int) Math.round(width * scale);

        int newHeight = (int) Math.round(height * scale);

        BufferedImage resized = new BufferedImage(
                newWidth,
                newHeight,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = resized.createGraphics();

        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        graphics.drawImage(
                source,
                0,
                0,
                newWidth,
                newHeight,
                null);

        graphics.dispose();

        return resized;
    }

    private byte[] convertToWebp(
            BufferedImage image) {

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType(
                "image/webp");

        if (!writers.hasNext()) {
            throw new ImageStorageException(
                    "No se encontró un encoder WebP");
        }

        ImageWriter writer = writers.next();

        try (
                ByteArrayOutputStream output = new ByteArrayOutputStream();

                ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {

            writer.setOutput(imageOutput);

            ImageWriteParam params = writer.getDefaultWriteParam();

            if (params.canWriteCompressed()) {

                params.setCompressionMode(
                        ImageWriteParam.MODE_EXPLICIT);

                String[] compressionTypes = params.getCompressionTypes();

                if (compressionTypes != null) {

                    for (String type : compressionTypes) {

                        if ("Lossy"
                                .equalsIgnoreCase(type)) {

                            params.setCompressionType(type);
                            break;
                        }
                    }
                }

                params.setCompressionQuality(
                        WEBP_QUALITY);
            }

            writer.write(
                    null,
                    new IIOImage(
                            image,
                            null,
                            null),
                    params);

            return output.toByteArray();

        } catch (IOException e) {

            throw new ImageStorageException(
                    "No se pudo convertir la imagen a WebP");

        } finally {

            writer.dispose();
        }
    }

    private void uploadToSupabase(
            String path,
            byte[] image) {

        String uploadUrl = supabaseUrl
                + "/storage/v1/object/"
                + bucket
                + "/"
                + path;

        try {

            restClient
                    .post()
                    .uri(uploadUrl)
                    .header(
                            "apikey",
                            secretKey)
                    .header("Authorization", "Bearer " + secretKey)
                    .header(
                            "x-upsert",
                            "false")
                    .contentType(
                            MediaType.valueOf(
                                    "image/webp"))
                    .body(image)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException e) {

            throw new ImageStorageException(
                    "No se pudo subir la imagen a Supabase Storage");
        }
    }

    private String buildPublicUrl(
            String path) {

        return supabaseUrl
                + "/storage/v1/object/public/"
                + bucket
                + "/"
                + path;
    }
}