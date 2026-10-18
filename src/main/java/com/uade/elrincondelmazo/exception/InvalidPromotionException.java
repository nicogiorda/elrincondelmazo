package com.uade.elrincondelmazo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPromotionException extends RuntimeException {
    // Excepción personalizada lanzada cuando los datos de una promoción
    // no cumplen las reglas de validación de negocio.
    // Se anota con @ResponseStatus(HttpStatus.BAD_REQUEST) para que Spring
    // devuelva un 400 al cliente cuando esta excepción sea propagada desde
    // un controlador REST.

    public InvalidPromotionException(String message) {
        super(message);
    }
}