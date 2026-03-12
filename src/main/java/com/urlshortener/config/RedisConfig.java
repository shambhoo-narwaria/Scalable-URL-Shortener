package com.urlshortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * =============================================================
 * REDIS CONFIGURATION
 * =============================================================
 *
 * WHAT IS REDIS?
 *   Redis = Remote Dictionary Server
 *   It's an in-memory key-value store.
 *
 *   Think of it as a super-fast HashMap that:
 *   - Lives separately from your app (separate process/server)
 *   - Survives app restarts (optional persistence)
 *   - Is shared across multiple app instances (critical for scaling)
 *   - Has built-in TTL (time-to-live / auto-expiry)
 *   - Can handle 100,000+ operations per second
 *
 * WHY NOT JUST USE A JAVA HASHMAP?
 *   1. Java HashMap is IN-PROCESS — only one app instance can use it.
 *      When you run 3 app instances (for scaling), each would have
 *      its own HashMap = inconsistent caches.
 *   2. HashMap dies when app restarts — all cached data lost.
 *   3. HashMap has no built-in TTL — you'd have to implement expiry manually.
 *   4. HashMap counts against your app's heap memory.
 *   Redis solves ALL of these problems.
 *
 * =============================================================
 * WHY IS THIS CLASS ALMOST EMPTY?
 * =============================================================
 * Spring Boot auto-configuration handles everything!
 * When it sees the spring-boot-starter-data-redis JAR, it:
 *   1. Reads redis.host and redis.port from application.yml
 *   2. Creates a LettuceConnectionFactory (Redis client)
 *   3. Auto-creates a StringRedisTemplate bean (String key/value)
 *
 * StringRedisTemplate (auto-created by Spring Boot):
 *   - Identical to RedisTemplate<String, String>
 *   - Already configured with StringRedisSerializer
 *   - Stores clean, readable text in Redis (not binary)
 *
 * LESSON LEARNED: Don't create a custom RedisTemplate<String, String>
 * because Spring Boot already provides StringRedisTemplate.
 * Defining both causes a "NoUniqueBeanDefinitionException"
 * (Spring can't tell which one to inject — ambiguous beans).
 *
 * Our RedisService injects StringRedisTemplate directly.
 * =============================================================
 */
@Configuration
public class RedisConfig {

    /**
     * =============================================================
     * RedisTemplate<String, String>
     * =============================================================
     * RedisTemplate is Spring's abstraction over the raw Jedis/Lettuce
     * Redis client. It provides:
     *   - Type-safe operations
     *   - Serialization/deserialization
     *   - Consistent exception translation
     *
     * WHY <String, String>?
     *   - KEY type:   String (e.g., "short:aB12x")
     *   - VALUE type: String (e.g., "https://example.com/...")
     *   - We only store URL strings, so String→String is perfect.
     *   - If you need to store complex objects, use <String, Object>
     *     with JSON serialization (Jackson2JsonRedisSerializer).
     *
     * SERIALIZATION PROBLEM (Why we configure this):
     *   By default, RedisTemplate uses Java serialization,
     *   which produces unreadable binary data in Redis like:
     *     \xac\xed\x00\x05t\x00\x08short:ab...
     *
     *   With StringRedisSerializer, Redis stores clean plain text:
     *     Key:   "short:aB12x"
     *     Value: "https://example.com/long/url"
     *
     *   This makes debugging with redis-cli much easier!
     *
     * @param factory  Auto-injected by Spring — configured from
     *                 application.yml (host, port, password)
     * =============================================================
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();

        // Connect to Redis (uses host/port from application.yml)
        template.setConnectionFactory(factory);

        // Use String serializer for both keys and values
        // This stores human-readable text in Redis instead of binary
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);

        // Also set hash serializers (for when we use Redis Hashes)
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        return template;
    }
}
