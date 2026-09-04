package com.uade.elrincondelmazo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ReviewAlreadyExistsException
        extends RuntimeException {

    public ReviewAlreadyExistsException(String message) {
        super(message);
    }
}