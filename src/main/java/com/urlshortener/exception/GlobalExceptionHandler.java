package com.urlshortener.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * =============================================================
 * GLOBAL EXCEPTION HANDLER
 * =============================================================
 *
 * @RestControllerAdvice — What is it?
 *   A special Spring annotation that makes this class an
 *   "interceptor" for exceptions thrown across ALL controllers.
 *
 *   Without this:
 *     If NotFoundException is thrown, Spring returns a generic
 *     Whitelabel Error Page or empty response — not useful.
 *
 *   With this:
 *     All exceptions are caught here and turned into clean
 *     JSON error responses that clients can parse.
 *
 * =============================================================
 * HOW IT WORKS:
 * =============================================================
 *
 *   1. Controller method throws NotFoundException
 *   2. Spring scans @RestControllerAdvice beans for @ExceptionHandler
 *      that matches NotFoundException.class
 *   3. Spring calls our handleNotFoundException() method
 *   4. We return a ResponseEntity with the right HTTP status + JSON body
 *
 * =============================================================
 * CONSISTENT ERROR RESPONSE FORMAT
 * =============================================================
 * All errors return this JSON structure:
 *
 * {
 *   "timestamp": "2026-03-10T23:00:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Short URL not found: aB12x"
 * }
 *
 * Consistent format = clients can always parse errors the same way.
 * =============================================================
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle short URL not found or expired.
     * HTTP 404 Not Found
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handle custom short code conflicts.
     * HTTP 409 Conflict
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handle invalid URL format.
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handle rate limit exceeded.
     * HTTP 429 Too Many Requests
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    /**
     * Handle @Valid annotation failures (input validation).
     *
     * When a controller method parameter annotated with @Valid fails,
     * Spring throws MethodArgumentNotValidException.
     *
     * This handler collects ALL field validation errors and returns
     * them in a structured format.
     *
     * Example response for invalid URL input:
     * {
     *   "timestamp": "...",
     *   "status": 400,
     *   "error": "Validation Failed",
     *   "fieldErrors": {
     *     "url": "URL must not be empty",
     *     "expiryDays": "Expiry days must be a positive number"
     *   }
     * }
     *
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // Collect all field errors into a map: { fieldName: errorMessage }
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed: {}", fieldErrors);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Catch-all handler for unexpected exceptions.
     * Prevents leaking internal error details to clients.
     * HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        // Log the full stack trace internally (for debugging)
        log.error("Unexpected error: ", ex);

        // Return a vague message to the client (security: don't expose internals)
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later."
        );
    }

    /**
     * Helper method to build a consistent error response body.
     *
     * Returns: ResponseEntity with JSON body:
     * {
     *   "timestamp": "2026-03-10T23:00:00",
     *   "status": 404,
     *   "error": "Not Found",
     *   "message": "..."
     * }
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String message) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return ResponseEntity.status(status).body(body);
    }
}
