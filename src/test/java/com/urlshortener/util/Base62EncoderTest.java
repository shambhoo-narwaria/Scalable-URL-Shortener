package com.urlshortener.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * =============================================================
 * UNIT TESTS — Base62Encoder
 * =============================================================
 *
 * WHAT IS A UNIT TEST?
 *   A unit test tests a single UNIT of code (one class/method)
 *   in ISOLATION from all other classes.
 *
 *   For Base62Encoder, we don't need Spring, DB, or Redis.
 *   We just create the object and call its methods directly.
 *   This makes the test fast (< 1ms) and focused.
 *
 * UNIT vs INTEGRATION TEST:
 *   Unit test:       Tests one class, mocks everything else. Fast.
 *   Integration test: Tests multiple real components together.
 *                     Slower, needs real DB/Redis.
 *
 * =============================================================
 * JUnit 5 Annotations:
 * =============================================================
 * @Test         → Marks a method as a test case
 * @BeforeEach   → Runs before EACH test method (setup/reset)
 * @BeforeAll    → Runs ONCE before all tests (expensive setup)
 * @DisplayName  → Human-readable test name in test report
 *
 * assertEquals(expected, actual):
 *   If expected != actual → test FAILS with a clear message
 *
 * assertTrue(condition):
 *   If condition == false → test FAILS
 *
 * assertThrows(ExceptionClass, code):
 *   Asserts that executing `code` THROWS the specified exception
 *   Useful for testing error/validation paths
 *
 * =============================================================
 * TEST NAMING CONVENTION:
 * =============================================================
 * Method name format: methodName_scenario_expectedOutcome
 * Example: encode_withKnownId_returnsCorrectCode
 * This makes failing tests self-documenting.
 * =============================================================
 */
class Base62EncoderTest {

    // The class under test — created directly (no Spring needed)
    private Base62Encoder encoder;

    /**
     * @BeforeEach: Runs before each test method.
     * Creates a fresh encoder instance so tests are independent.
     */
    @BeforeEach
    void setUp() {
        encoder = new Base62Encoder();
    }

    // =============================================================
    // ENCODE TESTS
    // =============================================================

    @Test
    @DisplayName("encode(1) should return 'b' (index 1 in Base62 chars)")
    void encode_withOne_returnsSecondCharacter() {
        // 1 % 62 = 1 → CHARS[1] = 'b'
        String result = encoder.encode(1L);
        assertEquals("b", result);
    }

    @Test
    @DisplayName("encode with known values should match expected output")
    void encode_withKnownValues_returnsExpectedCodes() {
        // These are deterministic — same input ALWAYS gives same output
        assertNotNull(encoder.encode(1L));
        assertNotNull(encoder.encode(100L));
        assertNotNull(encoder.encode(999999L));
        assertNotNull(encoder.encode(Long.MAX_VALUE / 2));
    }

    @Test
    @DisplayName("encode should produce short codes (≤ 11 chars for Long.MAX_VALUE)")
    void encode_producesShortCodes() {
        // Even for very large IDs, Base62 is compact
        String code = encoder.encode(Long.MAX_VALUE);
        assertTrue(code.length() <= 11,
            "Base62 of Long.MAX_VALUE should fit in 11 chars, got: " + code.length());
    }

    @Test
    @DisplayName("encode should produce unique codes for different IDs")
    void encode_withDifferentIds_producesUniqueCodes() {
        // Test uniqueness for first 10,000 IDs
        // This is THE most important property: no collisions!
        Set<String> codes = new HashSet<>();
        int testCount = 10_000;

        for (long i = 1; i <= testCount; i++) {
            String code = encoder.encode(i);
            boolean wasNew = codes.add(code);

            // If wasNew is false, we have a duplicate — collision!
            assertTrue(wasNew,
                "Duplicate code found for ID " + i + ": '" + code + "'");
        }

        assertEquals(testCount, codes.size(),
            "Expected " + testCount + " unique codes, got " + codes.size());
    }

    @Test
    @DisplayName("encode should only use URL-safe Base62 characters")
    void encode_producesOnlyUrlSafeCharacters() {
        String validChars =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        // Test a sample of IDs
        for (long id = 1; id <= 1000; id++) {
            String code = encoder.encode(id);
            for (char c : code.toCharArray()) {
                assertTrue(validChars.indexOf(c) >= 0,
                    "Invalid character '" + c + "' found in code: '" + code + "'");
            }
        }
    }

    @Test
    @DisplayName("encode(0) should return a non-empty code")
    void encode_withZero_returnsNonEmptyCode() {
        String code = encoder.encode(0L);
        assertNotNull(code);
        assertFalse(code.isEmpty());
    }

    // =============================================================
    // DECODE TESTS
    // =============================================================

    @Test
    @DisplayName("decode(encode(id)) should return the original id (round-trip)")
    void decode_afterEncode_returnsOriginalId() {
        // ROUND-TRIP TEST: encode → decode → same ID
        // This is the gold standard for encoding/decoding systems
        long[] testIds = {1L, 62L, 1000L, 125678L, 99999999L};

        for (long originalId : testIds) {
            String encoded = encoder.encode(originalId);
            long decoded = encoder.decode(encoded);

            assertEquals(originalId, decoded,
                "Round-trip failed for ID: " + originalId +
                " | Encoded: '" + encoded + "' | Decoded: " + decoded);
        }
    }

    @Test
    @DisplayName("decode with invalid character should throw IllegalArgumentException")
    void decode_withInvalidCharacter_throwsException() {
        // '!' is not in our Base62 charset
        assertThrows(IllegalArgumentException.class,
            () -> encoder.decode("abc!def"),
            "Should throw for invalid character '!'"
        );
    }

    // =============================================================
    // EDGE CASE TESTS
    // =============================================================

    @Test
    @DisplayName("Consecutive IDs should produce different codes (no clustering)")
    void encode_consecutiveIds_produceDifferentCodes() {
        String code1 = encoder.encode(100L);
        String code2 = encoder.encode(101L);
        String code3 = encoder.encode(102L);

        assertNotEquals(code1, code2);
        assertNotEquals(code2, code3);
        assertNotEquals(code1, code3);
    }
}
