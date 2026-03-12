package com.urlshortener.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * =============================================================
 * ANALYTICS RESPONSE DTO
 * =============================================================
 * Returned by: GET /api/analytics/{shortCode}
 *
 * This DTO aggregates information about a short URL's
 * usage statistics. In a real-world system, this could include
 * much more data (geographic distribution, browser types,
 * hourly breakdown, referrers, etc.)
 *
 * Example JSON response:
 * {
 *   "shortCode": "aB12x",
 *   "longUrl": "https://example.com/very/long/url",
 *   "clicks": 1243,
 *   "createdAt": "2026-03-10T12:00:00",
 *   "expiryDate": "2026-04-10T12:00:00"
 * }
 * =============================================================
 */
@Data
@Builder
public class AnalyticsResponse {

    /** The short code being analyzed */
    private String shortCode;

    /** The destination URL */
    private String longUrl;

    /** Total number of times this URL was accessed/clicked */
    private Long clicks;

    /** When this short URL was first created */
    private LocalDateTime createdAt;

    /**
     * When this URL expires (null = never expires).
     * Useful for clients displaying "expires in X days" UI.
     */
    private LocalDateTime expiryDate;
}
