package com.urlshortener.util;

import org.springframework.stereotype.Component;

/**
 * =============================================================
 * WHAT IS BASE62 ENCODING?
 * =============================================================
 * Base62 converts a number (Long/integer) into a short string
 * using 62 possible characters:
 *
 *   a-z = 26 characters  (indices 0-25)
 *   A-Z = 26 characters  (indices 26-51)
 *   0-9 = 10 characters  (indices 52-61)
 *   TOTAL = 62 characters
 *
 * WHY BASE62?
 *   - All characters are URL-safe (no /, ?, &, = etc.)
 *   - Short output: 6 chars = 62^6 = ~56.8 BILLION unique URLs
 *   - Deterministic: same ID always → same code (no randomness)
 *   - Fast: O(log62 n) time complexity ≈ O(log n)
 *
 * =============================================================
 * HOW IT WORKS — WITH EXAMPLE
 * =============================================================
 * It's like converting decimal (base 10) to binary (base 2),
 * but we use base 62 instead.
 *
 * Example: encode(125678)
 *
 *   125678 ÷ 62 = 2027 remainder 24  → CHARS[24] = 'y'
 *    2027 ÷ 62 =   32 remainder 43  → CHARS[43] = 'R'
 *      32 ÷ 62 =    0 remainder 32  → CHARS[32] = 'G'
 *
 *   Collected (reversed): "GRy"
 *
 * Each DB row ID → one unique Base62 string.
 * Because IDs auto-increment (1, 2, 3...), there are NO COLLISIONS.
 *
 * =============================================================
 * CAPACITY ANALYSIS
 * =============================================================
 *   5 chars → 62^5 = 916,132,832     (~916 million)
 *   6 chars → 62^6 = 56,800,235,584  (~56.8 billion)
 *   7 chars → 62^7 = 3,521,614,606,208 (~3.5 trillion)
 *
 * 6 characters is sufficient for any URL shortener.
 * =============================================================
 */
@Component  // Spring manages this as a bean, can be @Autowired anywhere
public class Base62Encoder {

    /**
     * The character set for Base62 encoding.
     * Index 0  = 'a', Index 25 = 'z'
     * Index 26 = 'A', Index 51 = 'Z'
     * Index 52 = '0', Index 61 = '9'
     */
    private static final String CHARS =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int BASE = 62;

    /**
     * Encode a long integer (DB row ID) into a Base62 short code.
     *
     * Algorithm: Repeated division by 62, collect remainders.
     *
     * @param id  The auto-incremented database row ID (must be > 0)
     * @return    Short alphanumeric code (e.g., "aZ91k")
     *
     * Visual trace for id=1:
     *   1 ÷ 62 = 0 remainder 1 → CHARS[1] = 'b'
     *   Loop ends (id becomes 0)
     *   Result: "b"
     *
     * Visual trace for id=62:
     *   62 ÷ 62 = 1 remainder 0 → CHARS[0] = 'a'
     *    1 ÷ 62 = 0 remainder 1 → CHARS[1] = 'b'
     *   Loop ends
     *   Result: "ba" (after reverse)
     */
    public String encode(long id) {
        // Handle edge case: if ID is somehow 0
        if (id <= 0) {
            return String.valueOf(CHARS.charAt(0)); // returns "a"
        }

        StringBuilder sb = new StringBuilder();

        // Keep dividing by 62 until quotient becomes 0
        while (id > 0) {
            int remainder = (int) (id % BASE);  // Get the remainder
            sb.append(CHARS.charAt(remainder)); // Map remainder to character
            id = id / BASE;                      // Reduce id for next iteration
        }

        // We collected remainders from LSB to MSB, so reverse to get correct code
        return sb.reverse().toString();
    }

    /**
     * Decode a Base62 string back to the original long integer.
     *
     * This is the INVERSE of encode().
     * Useful for debugging or alternative lookup strategies.
     *
     * Algorithm: For each character, multiply running total by 62
     * and add the character's index value.
     *
     * Example: decode("ba")
     *   Start: result = 0
     *   'b' → index=1:  result = 0 * 62 + 1  = 1
     *   'a' → index=0:  result = 1 * 62 + 0  = 62
     *   Return: 62  ✓ (matches encode(62) = "ba")
     *
     * @param code  The Base62 short code string
     * @return      The original database row ID
     */
    public long decode(String code) {
        long result = 0;

        for (char c : code.toCharArray()) {
            // Find the index of this character in our CHARS string
            int index = CHARS.indexOf(c);

            if (index == -1) {
                throw new IllegalArgumentException(
                    "Invalid Base62 character: '" + c + "'"
                );
            }

            // Shift result left (multiply by base) and add this digit's value
            result = result * BASE + index;
        }

        return result;
    }
}
