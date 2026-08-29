package com.uade.elrincondelmazo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ProductAccessDeniedException extends RuntimeException {

    public ProductAccessDeniedException(String message) {
        super(message);
    }
}