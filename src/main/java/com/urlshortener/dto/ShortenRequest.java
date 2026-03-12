package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * =============================================================
 * WHAT IS A DTO?
 * =============================================================
 * DTO = Data Transfer Object
 *
 * It represents the SHAPE of data coming INTO or going OUT of
 * your API. Think of it as the "contract" between your API
 * and its clients.
 *
 * WHY NOT USE UrlEntity DIRECTLY?
 *
 *   1. SEPARATION OF CONCERNS:
 *      - UrlEntity is a DB object (has DB-specific annotations, IDs)
 *      - ShortenRequest is an API object (has validation annotations)
 *      - Mixing them creates tight coupling between DB schema and API contract
 *
 *   2. SECURITY:
 *      - You don't want clients to accidentally set fields like
 *        `clickCount` or `id` directly.
 *      - DTO only exposes what the client is ALLOWED to send.
 *
 *   3. FLEXIBILITY:
 *      - Your API can evolve independently from your DB schema.
 *      - E.g., rename a DB column without breaking the API.
 *
 * =============================================================
 * VALIDATION ANNOTATIONS (spring-boot-starter-validation)
 * =============================================================
 * These are checked automatically when you put @Valid in
 * the controller method. If validation fails, Spring returns
 * HTTP 400 Bad Request with error details automatically.
 *
 * @NotBlank    → Field must not be null AND not empty/whitespace
 * @Size        → Length constraints
 * @Pattern     → Must match a regex
 * @Positive    → Number must be > 0
 * @Min/@Max    → Numeric range constraints
 * =============================================================
 */
@Data  // Lombok: generates getters, setters, toString, equals, hashCode
public class ShortenRequest {

    /**
     * The original long URL to shorten.
     *
     * @NotBlank ensures the client actually sends a URL.
     * message = "..." is the error message returned in the 400 response.
     *
     * We validate URL FORMAT in the service layer (more complex logic there).
     * Here we just make sure it's not empty.
     *
     * Example valid values:
     *   "https://example.com/very/long/path?param=value"
     *   "http://subdomain.example.co.in/page"
     */
    @NotBlank(message = "URL must not be empty")
    private String url;

    /**
     * Optional: custom short code chosen by the user.
     *
     * Examples:
     *   "myproject", "shambhoo", "sale2024"
     *
     * If null/empty → system auto-generates using Base62.
     * If provided → we check for conflicts and use it.
     *
     * @Size(max=20) → short codes > 20 chars would be impractical
     * @Pattern → Only alphanumeric + hyphens + underscores allowed
     *            (prevents injection attacks and invalid URL characters)
     */
    @Size(max = 20, message = "Custom code must be 20 characters or less")
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]*$",
        message = "Custom code can only contain letters, numbers, hyphens, and underscores"
    )
    private String customCode;  // Optional — null means auto-generate

    /**
     * Optional: number of days until this URL expires.
     *
     * Examples:
     *   null → URL never expires
     *   30   → URL expires in 30 days
     *   7    → URL expires in 7 days
     *
     * @Positive → Must be at least 1 day (can't expire in the past)
     */
    @Positive(message = "Expiry days must be a positive number")
    private Integer expiryDays;  // Optional — null means no expiry

}
