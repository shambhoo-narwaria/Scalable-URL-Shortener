package com.urlshortener.service;

import com.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * =============================================================
 * ANALYTICS SERVICE — Background Processing
 * =============================================================
 *
 * WHY IS THIS A SEPARATE CLASS?
 *   We previously had the @Async method inside UrlService.
 *   However, there is a "gotcha" in Spring Boot AOP (Aspect Oriented Programming):
 *   If you call an @Async or @Transactional method from ANOTHER method
 *   inside the SAME class, Spring's proxy is bypassed, and annotations
 *   are IGNORED!
 *
 *   UrlService.resolveUrl() → called UrlService.incrementClickCountAsync()
 *   The call was synchronous, and since resolveUrl is in a read-only
 *   transaction, the DB update threw an error (HTTP 500).
 *
 *   By putting the method in a separate service and injecting it,
 *   Spring intercepts the method call and correctly spans a new
 *   background thread and a new database transaction.
 *
 * =============================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final UrlRepository urlRepository;

    /**
     * Increment click count asynchronously.
     *
     * @Async:
     *   Runs in a separate thread.
     *
     * @Transactional:
     *   Opens a new writable transaction (since default is read-write).
     *
     * @param shortCode  The short code to update analytics for
     */
    @Async
    @Transactional
    public void incrementClickCountAsync(String shortCode) {
        log.debug("Async: Incrementing click count for short code: {}", shortCode);

        try {
            urlRepository.incrementClickCountByShortCode(shortCode);
        } catch (Exception e) {
            log.error("Failed to increment click count for short code {}: {}", shortCode, e.getMessage());
        }
    }
}
