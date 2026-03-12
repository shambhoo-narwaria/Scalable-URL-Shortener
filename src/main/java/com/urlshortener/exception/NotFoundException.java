package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * =============================================================
 * CUSTOM EXCEPTION — NotFoundException
 * =============================================================
 *
 * WHY CUSTOM EXCEPTIONS?
 *   Java has generic exceptions (RuntimeException, IllegalArgumentException)
 *   but they carry no HTTP status information.
 *
 *   Custom exceptions let you:
 *     1. Clearly express WHAT went wrong in the domain
 *     2. Carry the appropriate HTTP status code
 *     3. Be caught specifically in GlobalExceptionHandler
 *
 * @ResponseStatus(HttpStatus.NOT_FOUND)
 *   Tells Spring: if this exception bubbles up unhandled,
 *   automatically return HTTP 404.
 *   But we'll handle it explicitly in GlobalExceptionHandler
 *   for a richer error response body.
 *
 * extends RuntimeException:
 *   RuntimeException = "unchecked" exception.
 *   You don't need to declare it in method signatures with "throws".
 *   Perfect for domain errors that can happen anywhere in business logic.
 *
 * =============================================================
 * EXCEPTION HIERARCHY IN THIS PROJECT:
 * =============================================================
 *
 *   RuntimeException
 *     ├── NotFoundException    → HTTP 404
 *     ├── ConflictException    → HTTP 409
 *     └── RateLimitException  → HTTP 429
 *
 * =============================================================
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

    /**
     * Constructor that accepts an error message.
     *
     * Usage in code:
     *   throw new NotFoundException("Short URL not found: aB12x");
     *   throw new NotFoundException("URL has expired");
     *
     * @param message  Human-readable error description
     */
    public NotFoundException(String message) {
        super(message);
    }
}
