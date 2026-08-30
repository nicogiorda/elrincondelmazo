package com.uade.elrincondelmazo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a product is invalid.
 * El precio debe ser mayor a cero
 * El stock no puede ser negativo
 * El producto debe tener al menos una imagen
 * El nombre es obligatorio
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class InvalidProductException extends RuntimeException {

    public InvalidProductException(String message) {
        super(message);
    }
}