package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a custom short code is already taken.
 * Maps to HTTP 409 Conflict.
 *
 * Example trigger:
 *   User requests customCode = "shambhoo"
 *   That code already exists in the DB
 *   → throw new ConflictException("Short code 'shambhoo' is already taken")
 *   → Client receives HTTP 409 with JSON error body
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
