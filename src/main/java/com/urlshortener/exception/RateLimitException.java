package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a client exceeds the allowed request rate.
 * Maps to HTTP 429 Too Many Requests.
 *
 * Example trigger:
 *   IP 1.2.3.4 sends 101st request within 60 seconds
 *   → throw new RateLimitException("Rate limit exceeded. Try again in 45 seconds.")
 *   → Client receives HTTP 429
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
