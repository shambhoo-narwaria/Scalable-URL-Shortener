package com.urlshortener.repository;

import com.urlshortener.model.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * =============================================================
 * WHAT IS A REPOSITORY?
 * =============================================================
 * A Repository is the DATA ACCESS LAYER — the only place in your
 * app that directly talks to the database.
 *
 * ARCHITECTURE RULE:
 *   Controller → Service → Repository → Database
 *   Never access the DB from controller or service directly.
 *
 * =============================================================
 * JpaRepository<UrlEntity, Long>
 * =============================================================
 * By extending JpaRepository, Spring DATA JPA automatically
 * generates the following methods FOR YOU (no SQL needed!):
 *
 *   save(entity)          → INSERT or UPDATE
 *   findById(id)          → SELECT WHERE id = ?
 *   findAll()             → SELECT *
 *   deleteById(id)        → DELETE WHERE id = ?
 *   existsById(id)        → SELECT COUNT(*) where id = ?
 *   count()               → SELECT COUNT(*)
 *
 *   Generic type params:
 *     UrlEntity → the entity class this repo manages
 *     Long      → the type of the primary key (@Id field)
 *
 * =============================================================
 * DERIVED QUERY METHODS
 * =============================================================
 * Spring Data JPA can GENERATE SQL by parsing method names!
 *
 * Method:   findByShortCode(String shortCode)
 * Generated SQL:
 *   SELECT * FROM urls WHERE short_code = ?
 *
 * Method:   existsByShortCode(String shortCode)
 * Generated SQL:
 *   SELECT COUNT(*) > 0 FROM urls WHERE short_code = ?
 *
 * Method:   findByExpiryDateBefore(LocalDateTime date)
 * Generated SQL:
 *   SELECT * FROM urls WHERE expiry_date < ?
 *
 * Spring reads: find + By + ShortCode
 *   - "find"      → SELECT
 *   - "By"        → WHERE
 *   - "ShortCode" → short_code column (mapped from camelCase)
 *
 * =============================================================
 * @Repository annotation
 * =============================================================
 * Marks this as a Spring-managed bean.
 * Also enables Spring to translate DB exceptions into
 * Spring's DataAccessException hierarchy (cleaner error handling).
 * Actually, when extending JpaRepository, Spring Data auto-detects
 * this as a repo, so @Repository is optional but good practice.
 * =============================================================
 */
@Repository
public interface UrlRepository extends JpaRepository<UrlEntity, Long> {

    /**
     * Find a URL entity by its short code.
     *
     * Returns Optional<UrlEntity> instead of UrlEntity because:
     *   - The short code might not exist in the DB
     *   - Optional forces the caller to handle the "not found" case
     *   - Avoids accidental NullPointerException
     *
     * Usage:
     *   Optional<UrlEntity> result = repo.findByShortCode("aB12x");
     *   result.ifPresent(entity -> ...);
     *   result.orElseThrow(() -> new NotFoundException("..."));
     *
     * Generated SQL: SELECT * FROM urls WHERE short_code = ?
     */
    Optional<UrlEntity> findByShortCode(String shortCode);

    /**
     * Check if a short code already exists (for custom code validation).
     *
     * Returns boolean directly — simpler than Optional when
     * you only need yes/no.
     *
     * Generated SQL: SELECT COUNT(*) > 0 FROM urls WHERE short_code = ?
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Custom JPQL query to increment click count atomically.
     *
     * WHY USE @Query instead of find-then-save?
     *   The naive approach:
     *     entity.setClickCount(entity.getClickCount() + 1);
     *     repo.save(entity);
     *
     *   Problem: Under high concurrency (multiple servers), two threads
     *   could both READ clickCount=5, both set it to 6, and one
     *   increment is LOST. This is a "lost update" race condition.
     *
     *   The @Query approach generates:
     *     UPDATE urls SET click_count = click_count + 1 WHERE id = ?
     *   This is an ATOMIC database operation — the DB handles
     *   concurrency internally. No race condition!
     *
     * @Modifying → Required for UPDATE/DELETE queries (not SELECT)
     * @Transactional → Wraps in a transaction. Required for @Modifying.
     * @Param("id") → Binds the method parameter to the :id placeholder
     */
    @Modifying
    @Transactional
    @Query("UPDATE UrlEntity u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCountByShortCode(@Param("shortCode") String shortCode);
}
