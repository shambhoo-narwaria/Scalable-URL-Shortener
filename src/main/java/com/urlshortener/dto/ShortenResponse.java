package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * =============================================================
 * RESPONSE DTO — What the API returns to the client
 * =============================================================
 * This is what gets serialized to JSON and sent back in the
 * HTTP response body after a successful URL shortening.
 *
 * Jackson (the JSON library bundled with Spring Boot) converts
 * this Java object → JSON string automatically.
 *
 * Example JSON output:
 * {
 *   "shortUrl": "http://localhost:8080/aB12x",
 *   "shortCode": "aB12x",
 *   "longUrl": "https://example.com/very/long/url",
 *   "expiryDate": "2026-04-10T23:00:00"
 * }
 *
 * If expiryDate is null (no expiry), @JsonInclude(NON_NULL)
 * omits it from the JSON entirely instead of showing:
 *   "expiryDate": null
 *
 * =============================================================
 * @Builder Pattern (Lombok)
 * =============================================================
 * Without @Builder, you'd write:
 *   ShortenResponse response = new ShortenResponse();
 *   response.setShortUrl(url);
 *   response.setShortCode(code);
 *   // ... etc
 *
 * With @Builder:
 *   ShortenResponse response = ShortenResponse.builder()
 *       .shortUrl(url)
 *       .shortCode(code)
 *       .longUrl(originalUrl)
 *       .expiryDate(expiry)
 *       .build();
 *
 * The Builder pattern is:
 *   - More readable (each field is named)
 *   - Immutable-friendly (you can build and never modify)
 *   - Safe (compiler error if you forget required fields — see @NonNull)
 * =============================================================
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Omit null fields from JSON output
public class ShortenResponse {

    /**
     * The full short URL (base URL + "/" + short code)
     * Example: "http://localhost:8080/aB12x"
     * This is what the user will share/click.
     */
    private String shortUrl;

    /**
     * Just the code portion.
     * Useful if client wants to construct their own URLs
     * or call the analytics endpoint.
     * Example: "aB12x"
     */
    private String shortCode;

    /**
     * The original long URL (echo back for confirmation).
     * Example: "https://example.com/very/long/path"
     */
    private String longUrl;

    /**
     * When this short URL expires (null = never expires).
     * Jackson serializes LocalDateTime as: "2026-04-10T23:00:00"
     * (ISO 8601 format — universally understood)
     * Because of @JsonInclude(NON_NULL), if null, this field
     * won't appear in the JSON response at all.
     */
    private LocalDateTime expiryDate;
}
