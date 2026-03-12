package com.urlshortener.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * =============================================================
 * WHAT IS AN ENTITY?
 * =============================================================
 * An Entity is a Java class that represents a TABLE in the database.
 * Each INSTANCE of this class = one ROW in the "urls" table.
 * Each FIELD of this class = one COLUMN in the table.
 *
 * Hibernate (the JPA implementation) reads this class and:
 *   - Creates the SQL table for you (when ddl-auto=update)
 *   - Generates INSERT, SELECT, UPDATE, DELETE SQL automatically
 *
 * =============================================================
 * DATABASE TABLE THAT GETS CREATED:
 * =============================================================
 *
 *  CREATE TABLE urls (
 *    id          BIGSERIAL    PRIMARY KEY,
 *    short_code  VARCHAR(10)  NOT NULL UNIQUE,
 *    long_url    TEXT         NOT NULL,
 *    created_at  TIMESTAMP,
 *    expiry_date TIMESTAMP,
 *    click_count BIGINT       DEFAULT 0
 *  );
 *
 * =============================================================
 * LOMBOK ANNOTATIONS (reduces boilerplate)
 * =============================================================
 * @Data              → Generates: getters, setters, toString(), equals(), hashCode()
 * @Builder           → Generates builder pattern:
 *                        UrlEntity.builder().shortCode("abc").longUrl("...").build()
 * @NoArgsConstructor → Generates: public UrlEntity() {} — required by JPA/Hibernate
 * @AllArgsConstructor→ Generates: public UrlEntity(all fields) — used by @Builder
 *
 * =============================================================
 * JPA ANNOTATIONS
 * =============================================================
 * @Entity → Marks this class as a JPA-managed entity (maps to a DB table)
 * @Table  → Customizes the table name, adds index definitions
 * @Id     → Marks the field as the PRIMARY KEY
 * @GeneratedValue → Strategy for generating the PK value:
 *   - IDENTITY: DB auto-increments the ID (SERIAL in PostgreSQL)
 *   - SEQUENCE: Uses a DB sequence (more efficient for bulk inserts)
 * @Column → Customizes the column. Fields match column names by convention
 *           (camelCase → snake_case) but can be overridden.
 * @PrePersist → Method called by JPA before INSERT (perfect for timestamps)
 * =============================================================
 */
@Entity
@Table(
    name = "urls",
    indexes = {
        // This creates DB indexes for fast lookup
        // Without index: full table scan = O(n)
        // With index:    B-tree lookup    = O(log n)
        @Index(name = "idx_short_code", columnList = "short_code"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_expiry_date", columnList = "expiry_date")
    }
)
@Data                   // Lombok: getters + setters + equals + hashCode + toString
@Builder                // Lombok: enables UrlEntity.builder()...build() pattern
@NoArgsConstructor      // Lombok: public UrlEntity() {} — JPA requires this
@AllArgsConstructor     // Lombok: all-field constructor (needed by @Builder)
public class UrlEntity {

    /**
     * PRIMARY KEY
     * -----------
     * BIGSERIAL in PostgreSQL = auto-incrementing 64-bit integer.
     * Why BIGINT (Long) and not INT?
     *   INT max value = 2,147,483,647 (~2 billion)
     *   BIGINT max value = 9,223,372,036,854,775,807 (~9 quintillion)
     * For a popular URL shortener, we could easily exceed 2 billion URLs.
     * BIGINT is the safe choice.
     *
     * Why does ID choice matter for Base62?
     *   We encode the DB row ID into Base62.
     *   ID=1 → "b", ID=125678 → "aZ91k"
     *   Since DB auto-increments, IDs are always unique → codes are always unique.
     *   No random generation, no collision detection needed!
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SHORT CODE
     * ----------
     * The 5-7 character unique code (e.g., "aB12x")
     * unique = true → DB enforces no duplicates (even if app logic fails)
     * nullable = false → NOT NULL constraint in DB
     * length = 10 → VARCHAR(10) — enough for Base62 up to 56 billion entries
     */
    @Column(name = "short_code", nullable = false, unique = true, length = 10)
    private String shortCode;

    /**
     * LONG URL
     * --------
     * The original long URL to redirect to.
     * columnDefinition = "TEXT" because URLs can be very long
     * (regular VARCHAR has a 255 char limit in some DBs)
     * TEXT has no length limit.
     */
    @Column(name = "long_url", nullable = false, columnDefinition = "TEXT")
    private String longUrl;

    /**
     * CREATED_AT
     * ----------
     * When the short URL was created.
     * We auto-set this using @PrePersist — no need to set it manually.
     * Useful for:
     *   - Analytics queries ("URLs created this month")
     *   - Debugging
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * EXPIRY_DATE
     * -----------
     * Optional expiry. If null → URL never expires.
     * If set → URL returns 404 after this date.
     *
     * nullable = true (default) → column allows NULL values.
     * When user doesn't specify expiry days, this stays null.
     */
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    /**
     * CLICK_COUNT
     * -----------
     * Tracks how many times this short URL was accessed.
     * We use Long (64-bit) because a viral link could get
     * millions/billions of clicks.
     *
     * Note: We increment this asynchronously to not slow down redirects.
     * This means count may be slightly behind real-time (eventual consistency).
     * For analytics, that's perfectly acceptable.
     */
    @Column(name = "click_count")
    @Builder.Default        // Lombok: ensures default value works with @Builder
    private Long clickCount = 0L;

}
