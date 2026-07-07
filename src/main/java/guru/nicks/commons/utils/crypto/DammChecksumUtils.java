package guru.nicks.commons.utils.crypto;

import guru.nicks.commons.utils.text.TextUtils;

import am.ik.yavi.meta.ConstraintArguments;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;

/**
 *
 * Generates and validates <a href="https://en.wikipedia.org/wiki/Damm_algorithm">Damm checksums</a>.
 *
 * @see #DECIMAL
 * @see #CROCKFORD_BASE32
 */
public class DammChecksumUtils {

    /**
     * Operates within {@link TextUtils#DECIMAL_ALPHABET}, i.e. both the input and the checksum belong to it.
     */
    public static final Impl DECIMAL = new Impl(DammRadix.R10, TextUtils.DECIMAL_ALPHABET);

    /**
     * Operates within {@link TextUtils#CROCKFORD_BASE32_ALPHABET}, i.e. both the input and the checksum belong to it.
     */
    public static final Impl CROCKFORD_BASE32 = new Impl(DammRadix.R32, TextUtils.CROCKFORD_BASE32_ALPHABET);

    /**
     * From radixes not present in this enum, the tables need to be
     * <a href="http://www.md-software.de/math/DAMM_Quasigruppen.txt">downloaded manually</a>. It's possible to
     * generate such tables on the fly, but since multiple variants may exist, it's recommended to stick to the official
     * list.
     */
    @RequiredArgsConstructor
    public enum DammRadix {

        /**
         * Same table as in <a href="https://en.wikipedia.org/wiki/Damm_algorithm">Wikipedia</a>. It's also aligned with
         * the <a href="https://g5jda.uk/2020/09/damm-algorithm-check-digit-tool/">online check tool</a>.
         */
        R10(10, TextUtils.DECIMAL_ALPHABET, new int[][]{
                {0, 3, 1, 7, 5, 9, 8, 6, 4, 2},
                {7, 0, 9, 2, 1, 5, 4, 8, 6, 3},
                {4, 2, 0, 6, 8, 7, 1, 3, 5, 9},
                {1, 7, 5, 0, 9, 8, 3, 4, 2, 6},
                {6, 1, 2, 3, 0, 4, 5, 9, 7, 8},
                {3, 6, 7, 4, 2, 0, 9, 5, 8, 1},
                {5, 8, 6, 9, 7, 2, 0, 1, 3, 4},
                {8, 9, 4, 5, 3, 6, 2, 0, 1, 7},
                {9, 4, 3, 8, 6, 1, 7, 2, 0, 5},
                {2, 5, 8, 1, 4, 3, 6, 7, 9, 0}
        }),

        R32(32, TextUtils.CROCKFORD_BASE32_ALPHABET, new int[][]{
                {0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 3, 1, 7, 5, 11, 9, 15, 13, 19, 17,
                        23, 21, 27, 25, 31, 29},
                {2, 0, 6, 4, 10, 8, 14, 12, 18, 16, 22, 20, 26, 24, 30, 28, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19,
                        21, 23, 25, 27, 29, 31},
                {4, 6, 0, 2, 12, 14, 8, 10, 20, 22, 16, 18, 28, 30, 24, 26, 7, 5, 3, 1, 15, 13, 11, 9, 23, 21,
                        19, 17, 31, 29, 27, 25},
                {6, 4, 2, 0, 14, 12, 10, 8, 22, 20, 18, 16, 30, 28, 26, 24, 5, 7, 1, 3, 13, 15, 9, 11, 21, 23,
                        17, 19, 29, 31, 25, 27},
                {8, 10, 12, 14, 0, 2, 4, 6, 24, 26, 28, 30, 16, 18, 20, 22, 11, 9, 15, 13, 3, 1, 7, 5, 27, 25,
                        31, 29, 19, 17, 23, 21},
                {10, 8, 14, 12, 2, 0, 6, 4, 26, 24, 30, 28, 18, 16, 22, 20, 9, 11, 13, 15, 1, 3, 5, 7, 25, 27,
                        29, 31, 17, 19, 21, 23},
                {12, 14, 8, 10, 4, 6, 0, 2, 28, 30, 24, 26, 20, 22, 16, 18, 15, 13, 11, 9, 7, 5, 3, 1, 31, 29,
                        27, 25, 23, 21, 19, 17},
                {14, 12, 10, 8, 6, 4, 2, 0, 30, 28, 26, 24, 22, 20, 18, 16, 13, 15, 9, 11, 5, 7, 1, 3, 29, 31,
                        25, 27, 21, 23, 17, 19},
                {16, 18, 20, 22, 24, 26, 28, 30, 0, 2, 4, 6, 8, 10, 12, 14, 19, 17, 23, 21, 27, 25, 31, 29, 3,
                        1, 7, 5, 11, 9, 15, 13},
                {18, 16, 22, 20, 26, 24, 30, 28, 2, 0, 6, 4, 10, 8, 14, 12, 17, 19, 21, 23, 25, 27, 29, 31, 1,
                        3, 5, 7, 9, 11, 13, 15},
                {20, 22, 16, 18, 28, 30, 24, 26, 4, 6, 0, 2, 12, 14, 8, 10, 23, 21, 19, 17, 31, 29, 27, 25, 7,
                        5, 3, 1, 15, 13, 11, 9},
                {22, 20, 18, 16, 30, 28, 26, 24, 6, 4, 2, 0, 14, 12, 10, 8, 21, 23, 17, 19, 29, 31, 25, 27, 5,
                        7, 1, 3, 13, 15, 9, 11},
                {24, 26, 28, 30, 16, 18, 20, 22, 8, 10, 12, 14, 0, 2, 4, 6, 27, 25, 31, 29, 19, 17, 23, 21, 11,
                        9, 15, 13, 3, 1, 7, 5},
                {26, 24, 30, 28, 18, 16, 22, 20, 10, 8, 14, 12, 2, 0, 6, 4, 25, 27, 29, 31, 17, 19, 21, 23, 9,
                        11, 13, 15, 1, 3, 5, 7},
                {28, 30, 24, 26, 20, 22, 16, 18, 12, 14, 8, 10, 4, 6, 0, 2, 31, 29, 27, 25, 23, 21, 19, 17, 15,
                        13, 11, 9, 7, 5, 3, 1},
                {30, 28, 26, 24, 22, 20, 18, 16, 14, 12, 10, 8, 6, 4, 2, 0, 29, 31, 25, 27, 21, 23, 17, 19, 13,
                        15, 9, 11, 5, 7, 1, 3},
                {3, 1, 7, 5, 11, 9, 15, 13, 19, 17, 23, 21, 27, 25, 31, 29, 0, 2, 4, 6, 8, 10, 12, 14, 16, 18,
                        20, 22, 24, 26, 28, 30},
                {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 2, 0, 6, 4, 10, 8, 14, 12, 18, 16,
                        22, 20, 26, 24, 30, 28},
                {7, 5, 3, 1, 15, 13, 11, 9, 23, 21, 19, 17, 31, 29, 27, 25, 4, 6, 0, 2, 12, 14, 8, 10, 20, 22,
                        16, 18, 28, 30, 24, 26},
                {5, 7, 1, 3, 13, 15, 9, 11, 21, 23, 17, 19, 29, 31, 25, 27, 6, 4, 2, 0, 14, 12, 10, 8, 22, 20,
                        18, 16, 30, 28, 26, 24},
                {11, 9, 15, 13, 3, 1, 7, 5, 27, 25, 31, 29, 19, 17, 23, 21, 8, 10, 12, 14, 0, 2, 4, 6, 24, 26,
                        28, 30, 16, 18, 20, 22},
                {9, 11, 13, 15, 1, 3, 5, 7, 25, 27, 29, 31, 17, 19, 21, 23, 10, 8, 14, 12, 2, 0, 6, 4, 26, 24,
                        30, 28, 18, 16, 22, 20},
                {15, 13, 11, 9, 7, 5, 3, 1, 31, 29, 27, 25, 23, 21, 19, 17, 12, 14, 8, 10, 4, 6, 0, 2, 28, 30,
                        24, 26, 20, 22, 16, 18},
                {13, 15, 9, 11, 5, 7, 1, 3, 29, 31, 25, 27, 21, 23, 17, 19, 14, 12, 10, 8, 6, 4, 2, 0, 30, 28,
                        26, 24, 22, 20, 18, 16},
                {19, 17, 23, 21, 27, 25, 31, 29, 3, 1, 7, 5, 11, 9, 15, 13, 16, 18, 20, 22, 24, 26, 28, 30, 0,
                        2, 4, 6, 8, 10, 12, 14},
                {17, 19, 21, 23, 25, 27, 29, 31, 1, 3, 5, 7, 9, 11, 13, 15, 18, 16, 22, 20, 26, 24, 30, 28, 2,
                        0, 6, 4, 10, 8, 14, 12},
                {23, 21, 19, 17, 31, 29, 27, 25, 7, 5, 3, 1, 15, 13, 11, 9, 20, 22, 16, 18, 28, 30, 24, 26, 4,
                        6, 0, 2, 12, 14, 8, 10},
                {21, 23, 17, 19, 29, 31, 25, 27, 5, 7, 1, 3, 13, 15, 9, 11, 22, 20, 18, 16, 30, 28, 26, 24, 6,
                        4, 2, 0, 14, 12, 10, 8},
                {27, 25, 31, 29, 19, 17, 23, 21, 11, 9, 15, 13, 3, 1, 7, 5, 24, 26, 28, 30, 16, 18, 20, 22, 8,
                        10, 12, 14, 0, 2, 4, 6},
                {25, 27, 29, 31, 17, 19, 21, 23, 9, 11, 13, 15, 1, 3, 5, 7, 26, 24, 30, 28, 18, 16, 22, 20, 10,
                        8, 14, 12, 2, 0, 6, 4},
                {31, 29, 27, 25, 23, 21, 19, 17, 15, 13, 11, 9, 7, 5, 3, 1, 28, 30, 24, 26, 20, 22, 16, 18, 12,
                        14, 8, 10, 4, 6, 0, 2},
                {29, 31, 25, 27, 21, 23, 17, 19, 13, 15, 9, 11, 5, 7, 1, 3, 30, 28, 26, 24, 22, 20, 18, 16, 14,
                        12, 10, 8, 6, 4, 2, 0}
        });

        @Getter(AccessLevel.PRIVATE)
        private final int radix;

        @Getter(AccessLevel.PRIVATE)
        private final String alphabet;

        /**
         * The getter is PRIVATE and returns the ORIGINAL array. The array content is protected from modification
         * because the enum field and getter are private - accessible from inner classes within the same outer class.
         */
        @Getter(AccessLevel.PRIVATE)
        private final int[][] dammTable;

    }

    public static class Impl {

        /**
         * Character-to-index mapping for O(1) lookups. Maps ASCII character codes to their index in the alphabet. Uses
         * -1 to indicate characters not in the alphabet (to mimic {@link String#indexOf(int)} which it replaces,
         * improving performance from O(n²) to O(n)). The size is 256 to cover all extended ASCII characters.
         */
        private final int[] charIndexesInAlphabet = new int[256];

        private final String alphabet;
        int[][] dammTable;

        /**
         * Note that {@code dammRadix} does not imply any specific alphabet, e.g. any 10 letters can be used under the
         * radix of 10.
         *
         * @param dammRadix radix of the Damm checksum (equals the alphabet length)
         * @param alphabet  alphabet of the Damm checksum, must not contain duplicates or characters outside the
         *                  extended ASCII range (0..255)
         */
        @ConstraintArguments
        public Impl(DammRadix dammRadix, String alphabet) {
            // see outer class comment
            check(alphabet, _DammChecksumUtils_ImplArgumentsMeta.ALPHABET.name())
                    .lengthBetweenInclusive(dammRadix.getRadix(), dammRadix.getRadix())
                    .constraint(it -> it.chars().distinct().count() == it.length(), "must not contain duplicates");
            this.alphabet = alphabet;

            Arrays.fill(charIndexesInAlphabet, -1);
            // for each character in the alphabet, store its index
            for (int i = 0; i < alphabet.length(); i++) {
                char chr = alphabet.charAt(i);

                if (chr > 255) {
                    throw new IllegalArgumentException("Alphabet must contain extended ASCII characters only (0..255)");
                }

                charIndexesInAlphabet[chr] = i;
            }

            dammTable = dammRadix.getDammTable();
        }

        /**
         * Computes the Damm check character.
         *
         * @param payload payload to compute the check character for
         * @return check character strictly within the given alphabet
         * @throws IllegalArgumentException if a character in the payload is not in the alphabet
         */
        public char compute(String payload) {
            int interim = 0;

            for (int i = 0; i < payload.length(); i++) {
                int charIndex = getCharIndexInAlphabet(payload.charAt(i));

                if (charIndex == -1) {
                    throw new IllegalArgumentException("Input character not in alphabet");
                }

                interim = dammTable[interim][charIndex];
            }

            return alphabet.charAt(interim);
        }

        /**
         * Validates the given string. Very fast: runs in O(n) with zero allocations.
         *
         * @param str string to validate (payload + checksum)
         */
        public boolean isValid(String str) {
            // fail-fast: blank or no room for payload+checksum
            if (StringUtils.isBlank(str) || (str.length() < 2)) {
                return false;
            }

            int interim = 0;

            for (int i = 0; i < str.length(); i++) {
                int charIndex = getCharIndexInAlphabet(str.charAt(i));

                // character not in alphabet
                if (charIndex == -1) {
                    return false;
                }

                interim = dammTable[interim][charIndex];
            }

            return interim == 0;
        }

        /**
         * Returns the index of the given character in the alphabet.
         *
         * @param chr character
         * @return index, or -1 if the character is not in the alphabet
         */
        private int getCharIndexInAlphabet(char chr) {
            return (chr < charIndexesInAlphabet.length)
                    ? charIndexesInAlphabet[chr]
                    : -1;
        }

    }

}
