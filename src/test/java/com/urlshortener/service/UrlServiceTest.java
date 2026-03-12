package com.urlshortener.service;

import com.urlshortener.cache.RedisService;
import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.exception.ConflictException;
import com.urlshortener.exception.NotFoundException;
import com.urlshortener.model.UrlEntity;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * =============================================================
 * UNIT TESTS — UrlService
 * =============================================================
 *
 * WHAT IS MOCKING?
 *   UrlService depends on:
 *     - UrlRepository (needs DB)
 *     - RedisService  (needs Redis)
 *     - Base62Encoder (pure Java, no deps)
 *
 *   In a UNIT test, we don't want a real DB or Redis.
 *   Too slow, too fragile, too hard to control.
 *
 *   Solution: MOCK the dependencies.
 *   A mock is a FAKE object that:
 *     - Has the same interface as the real object
 *     - Does nothing by default
 *     - Can be configured to return whatever we want
 *     - Records all calls made to it (for verification)
 *
 * MOCKITO is Java's most popular mocking library.
 *
 * =============================================================
 * Key Mockito Annotations:
 * =============================================================
 *
 * @ExtendWith(MockitoExtension.class):
 *   Enables Mockito in JUnit 5. Required for @Mock to work.
 *
 * @Mock:
 *   Creates a FAKE/MOCK version of the class.
 *   Example: @Mock UrlRepository urlRepository
 *   → Creates a fake UrlRepository that does nothing unless told to.
 *
 * @InjectMocks:
 *   Creates the REAL class under test, and INJECTS the @Mock objects
 *   into it via constructor/setter/field injection.
 *   → Creates real UrlService with mock UrlRepository, RedisService injected.
 *
 * =============================================================
 * Key Mockito Methods:
 * =============================================================
 *
 * when(mock.method(arg)).thenReturn(value):
 *   "When this method is called with this argument,
 *    return this value."
 *   Configures the mock's behavior.
 *
 * verify(mock).method(arg):
 *   "Assert that this method was called with this argument."
 *   Verifies interactions happened.
 *
 * verify(mock, never()).method(arg):
 *   "Assert that this method was NEVER called."
 *
 * any(), anyString(), anyLong():
 *   Argument matchers — match any value of that type.
 * =============================================================
 */
@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    // MOCKS — fake dependencies
    @Mock
    private UrlRepository urlRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private AnalyticsService analyticsService;

    // REAL instance — the class we're actually testing
    @InjectMocks
    private UrlService urlService;

    // Real Base62Encoder (no deps, safe to use real one)
    private Base62Encoder base62Encoder;

    @BeforeEach
    void setUp() {
        base62Encoder = new Base62Encoder();

        // Inject the real Base62Encoder into urlService
        // (since @InjectMocks won't inject it unless mocked)
        ReflectionTestUtils.setField(urlService, "base62Encoder", base62Encoder);

        // Set the baseUrl @Value field (normally set from application.yml)
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080");
    }

    // =============================================================
    // shortenUrl() TESTS
    // =============================================================

    @Test
    @DisplayName("shortenUrl should save to DB and return a short URL")
    void shortenUrl_withValidUrl_returnsShortUrl() {
        // ARRANGE — set up the scenario

        // The request coming from the client
        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://example.com/very/long/path");

        // What the DB returns after saving (ID = 42)
        UrlEntity savedEntity = UrlEntity.builder()
            .id(42L)
            .longUrl("https://example.com/very/long/path")
            .shortCode(base62Encoder.encode(42L)) // "Q" for 42
            .build();

        // Configure mock behavior:
        // When repo.save() is called with any UrlEntity, return savedEntity
        when(urlRepository.save(any(UrlEntity.class))).thenReturn(savedEntity);

        // ACT — call the method under test
        ShortenResponse response = urlService.shortenUrl(request);

        // ASSERT — verify the results
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getShortUrl(), "Short URL should not be null");
        assertTrue(response.getShortUrl().startsWith("http://localhost:8080/"),
            "Short URL should start with base URL");
        assertEquals("https://example.com/very/long/path", response.getLongUrl());

        // VERIFY — assert interactions with mocks
        // repo.save() should have been called at least once
        verify(urlRepository, atLeast(1)).save(any(UrlEntity.class));

        // Redis should have been populated with the cache
        verify(redisService).cacheUrl(anyString(), eq("https://example.com/very/long/path"));
    }

    @Test
    @DisplayName("shortenUrl with custom code should use the provided code")
    void shortenUrl_withCustomCode_usesCustomCode() {
        // ARRANGE
        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://example.com/page");
        request.setCustomCode("myalias");

        // Custom code doesn't exist yet
        when(urlRepository.existsByShortCode("myalias")).thenReturn(false);

        UrlEntity savedEntity = UrlEntity.builder()
            .id(1L)
            .longUrl("https://example.com/page")
            .shortCode("myalias")
            .build();

        when(urlRepository.save(any(UrlEntity.class))).thenReturn(savedEntity);

        // ACT
        ShortenResponse response = urlService.shortenUrl(request);

        // ASSERT
        assertNotNull(response);
        assertTrue(response.getShortUrl().contains("myalias"),
            "Short URL should contain the custom code");

        // Custom code check should have been called exactly once
        verify(urlRepository, times(1)).existsByShortCode("myalias");
    }

    @Test
    @DisplayName("shortenUrl with taken custom code should throw ConflictException")
    void shortenUrl_withTakenCustomCode_throwsConflictException() {
        // ARRANGE
        ShortenRequest request = new ShortenRequest();
        request.setUrl("https://example.com/page");
        request.setCustomCode("taken");

        // Simulate: custom code already exists in DB
        when(urlRepository.existsByShortCode("taken")).thenReturn(true);

        // ACT + ASSERT
        // assertThrows() verifies that the code throws the expected exception
        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> urlService.shortenUrl(request),
            "Should throw ConflictException for taken custom code"
        );

        assertTrue(exception.getMessage().contains("taken"),
            "Exception message should mention the code name");

        // DB save should NEVER have been called (rejected early)
        verify(urlRepository, never()).save(any());
    }

    @Test
    @DisplayName("shortenUrl with invalid URL should throw IllegalArgumentException")
    void shortenUrl_withInvalidUrl_throwsIllegalArgumentException() {
        ShortenRequest request = new ShortenRequest();
        request.setUrl("not-a-valid-url");

        assertThrows(
            IllegalArgumentException.class,
            () -> urlService.shortenUrl(request)
        );

        // No DB operations should happen for invalid URLs
        verify(urlRepository, never()).save(any());
        verify(redisService, never()).cacheUrl(anyString(), anyString());
    }

    // =============================================================
    // resolveUrl() TESTS
    // =============================================================

    @Test
    @DisplayName("resolveUrl with cache HIT should return URL without DB query")
    void resolveUrl_withCacheHit_returnsFromCacheWithoutDbQuery() {
        // ARRANGE
        String shortCode = "aB12x";
        String expectedUrl = "https://example.com/original";

        // Cache returns the URL (cache hit)
        when(redisService.getCachedUrl(shortCode))
            .thenReturn(Optional.of(expectedUrl));

        // ACT
        String result = urlService.resolveUrl(shortCode);

        // ASSERT
        assertEquals(expectedUrl, result);

        // CRITICAL: DB should NOT have been queried (cache was sufficient)
        verify(urlRepository, never()).findByShortCode(anyString());

        // This is the whole point of caching — verify it's working!
    }

    @Test
    @DisplayName("resolveUrl with cache MISS should query DB and populate cache")
    void resolveUrl_withCacheMiss_queriesDbAndPopulatesCache() {
        // ARRANGE
        String shortCode = "aB12x";
        String expectedUrl = "https://example.com/original";

        // Cache miss
        when(redisService.getCachedUrl(shortCode)).thenReturn(Optional.empty());

        // DB returns the entity
        UrlEntity entity = UrlEntity.builder()
            .id(1L)
            .shortCode(shortCode)
            .longUrl(expectedUrl)
            .build();
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(entity));

        // ACT
        String result = urlService.resolveUrl(shortCode);

        // ASSERT
        assertEquals(expectedUrl, result);

        // DB was queried (expected, it was a cache miss)
        verify(urlRepository).findByShortCode(shortCode);

        // Cache was populated for future requests
        verify(redisService).cacheUrl(shortCode, expectedUrl);
    }

    @Test
    @DisplayName("resolveUrl with unknown short code should throw NotFoundException")
    void resolveUrl_withUnknownCode_throwsNotFoundException() {
        // ARRANGE
        String shortCode = "unknown";
        when(redisService.getCachedUrl(shortCode)).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
            NotFoundException.class,
            () -> urlService.resolveUrl(shortCode)
        );
    }
}
