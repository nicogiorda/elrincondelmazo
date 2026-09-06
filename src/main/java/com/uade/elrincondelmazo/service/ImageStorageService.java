package com.uade.elrincondelmazo.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    String uploadProductImage(
            Long userId,
            MultipartFile file);
}