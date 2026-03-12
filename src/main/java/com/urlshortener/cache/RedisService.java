package com.urlshortener.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * =============================================================
 * REDIS SERVICE — Cache-Aside Pattern Implementation
 * =============================================================
 *
 * WHAT IS CACHE-ASIDE (also called "Lazy Loading")?
 *
 * The application code is responsible for cache management.
 * The cache sits "aside" and is only populated on demand.
 *
 * FLOW:
 *
 *   READ request:
 *     1. Check Redis cache first
 *     2. If CACHE HIT  → return immediately (fast!)
 *     3. If CACHE MISS → query DB, then WRITE to cache, then return
 *
 *   WRITE request:
 *     1. Write to DB (source of truth)
 *     2. Also write to cache (eager population)
 *     3. Future reads will be cache hits
 *
 * WHY CACHE-ASIDE INSTEAD OF WRITE-THROUGH?
 *
 *   Write-Through (alternative):
 *     Every write goes to both DB and cache simultaneously.
 *     Pro: cache is always current.
 *     Con: cache gets populated with data that may never be read,
 *          wasting memory.
 *
 *   Cache-Aside / Lazy Loading:
 *     Cache is only populated when data is actually READ.
 *     Pro: only "hot" (frequently accessed) data lives in cache.
 *     Con: first request after miss is slow (goes to DB).
 *
 *   For URL shorteners, most URLs have unpredictable access patterns.
 *   Some go viral (millions of hits), others are clicked once.
 *   Cache-Aside naturally keeps hot URLs in cache without
 *   wasting memory on cold ones.
 *
 * =============================================================
 * REDIS KEY DESIGN
 * =============================================================
 * We prefix all URL cache keys with "short:"
 *
 *   Key:   "short:aB12x"
 *   Value: "https://example.com/very/long/url"
 *
 * WHY PREFIX?
 *   Redis is a shared server. Prefixes act as namespaces:
 *     "short:aB12x"         → URL lookup cache
 *     "ratelimit:192.168.1.1" → Rate limiting counter
 *
 *   This avoids key collisions between different features.
 *
 * =============================================================
 * @Slf4j (Lombok logging)
 * =============================================================
 * Generates: private static final Logger log = LoggerFactory.getLogger(...)
 * Usage:
 *   log.info("Cache HIT for key: {}", key);   // INFO level
 *   log.warn("Cache MISS for key: {}", key);  // WARN level
 *   log.error("Redis error: {}", ex.getMessage()); // ERROR level
 * =============================================================
 */
@Service
@RequiredArgsConstructor  // Lombok: injects final fields via constructor
@Slf4j                    // Lombok: provides log.info(), log.warn() etc.
public class RedisService {

    // Spring injects Spring Boot's auto-configured StringRedisTemplate
    // StringRedisTemplate = RedisTemplate<String, String> with String serializers pre-set
    private final StringRedisTemplate redisTemplate;

    // Read cache TTL from application.yml
    // Default = 24 if the property is not set
    @Value("${app.cache.url-ttl-hours:24}")
    private long urlTtlHours;

    /** Namespace prefix for URL cache keys */
    private static final String URL_PREFIX = "short:";

    /**
     * Store a URL mapping in Redis cache.
     *
     * Called in two scenarios:
     *   1. After a new short URL is created (eager population)
     *   2. After a DB lookup on cache miss (lazy population)
     *
     * @param shortCode  The short code (e.g., "aB12x")
     * @param longUrl    The original long URL to cache
     */
    public void cacheUrl(String shortCode, String longUrl) {
        String key = URL_PREFIX + shortCode;
        Duration ttl = Duration.ofHours(urlTtlHours);

        /*
         * redisTemplate.opsForValue() → operations on simple String values
         * .set(key, value, ttl)       → SET key value EX seconds
         *
         * Without TTL, cached URLs would NEVER expire.
         * TTL ensures stale data eventually flushes itself out.
         * Also important for expired URLs: once the URL's expiry_date
         * passes, the cache entry TTL ensures it'll be gone eventually.
         *
         * For expired URLs, we immediately evict the cache (see evict()).
         */
        try {
            redisTemplate.opsForValue().set(key, longUrl, ttl);
            log.info("Cached URL | Key: {} | TTL: {}h", key, urlTtlHours);
        } catch (Exception e) {
            // If Redis is down, DON'T crash the application.
            // Log the error and continue — the app will fall back to DB queries.
            log.warn("Failed to cache URL in Redis: {}. Continuing without cache.", e.getMessage());
        }
    }

    /**
     * Retrieve a cached long URL from Redis.
     *
     * Returns Optional to force the caller to handle the "not in cache" case.
     *
     * @param shortCode  The short code to look up
     * @return           Optional<String> with the long URL if cached
     */
    public Optional<String> getCachedUrl(String shortCode) {
        String key = URL_PREFIX + shortCode;

        try {
            String value = redisTemplate.opsForValue().get(key);

            if (value != null) {
                log.info("Cache HIT  | Key: {}", key);
                return Optional.of(value);
            } else {
                log.info("Cache MISS | Key: {} — will query DB", key);
                return Optional.empty();
            }

        } catch (Exception e) {
            // Redis is down: return empty → caller will query DB instead
            log.warn("Redis unavailable for GET. Falling back to DB. Error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Remove a cached URL entry from Redis.
     *
     * Called when:
     *   - A URL is deleted (if we add that feature)
     *   - A URL expires (we actively evict it)
     *
     * @param shortCode  The short code to evict from cache
     */
    public void evict(String shortCode) {
        String key = URL_PREFIX + shortCode;
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("Cache EVICT | Key: {} | Deleted: {}", key, deleted);
        } catch (Exception e) {
            log.warn("Failed to evict key from Redis: {}. Error: {}", key, e.getMessage());
        }
    }

    /**
     * Rate limiting: Increment and check request count for an IP.
     *
     * Redis command:  INCR ratelimit:192.168.1.1
     * On first call:  EXPIRE ratelimit:192.168.1.1 60
     *
     * HOW IT WORKS:
     *   - First request from IP: counter created = 1, TTL = 60s
     *   - Each request:          counter incremented
     *   - After 60s:             key auto-expires, counter resets
     *
     * WHY REDIS FOR RATE LIMITING?
     *   If you stored counters in a Java HashMap:
     *     - Each app instance would have its own counter
     *     - User could bypass limit by sending requests to different instances
     *   Redis is SHARED across all instances → true global rate limiting
     *
     * @param clientIp     The client's IP address
     * @param maxRequests  Maximum allowed requests in the window
     * @param windowMinutes Time window in minutes
     * @return true if allowed, false if rate limit exceeded
     */
    public boolean isRateLimitAllowed(String clientIp, int maxRequests, int windowMinutes) {
        String key = "ratelimit:" + clientIp;

        try {
            // INCR is atomic — thread-safe and process-safe in Redis
            Long count = redisTemplate.opsForValue().increment(key);

            if (count != null && count == 1) {
                // First request in this window — set the expiry
                redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
            }

            boolean allowed = count != null && count <= maxRequests;

            if (!allowed) {
                log.warn("Rate limit exceeded for IP: {} | Count: {}/{}", clientIp, count, maxRequests);
            }

            return allowed;

        } catch (Exception e) {
            // If Redis is down, ALLOW the request (fail-open strategy)
            // Alternative: fail-closed (block all requests when Redis down)
            // For a URL shortener, fail-open is more user-friendly
            log.error("Rate limiter Redis error — allowing request: {}", e.getMessage());
            return true;
        }
    }
}
