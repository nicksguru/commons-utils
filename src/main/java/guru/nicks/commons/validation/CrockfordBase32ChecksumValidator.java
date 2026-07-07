package guru.nicks.commons.validation;

import guru.nicks.commons.utils.crypto.DammChecksumUtils;
import guru.nicks.commons.utils.text.TextUtils;

import am.ik.yavi.meta.ConstraintArguments;
import jakarta.annotation.Nullable;

import java.util.function.Predicate;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Validates input with respect to:
 * <ul>
 *     <li>min. length (passed to constructor)</li>
 *     <li>max. length (passed to constructor)</li>
 *     <li>payload content ({@link TextUtils#CROCKFORD_BASE32_ALPHABET}), but unlike the original Crockford Base32,
 *         <b>input is treated case-sensitively</b> - non-uppercase characters are invalid</li>
 *     <li>{@link DammChecksumUtils#CROCKFORD_BASE32 Damm check digit} whose advantage is even distribution across
 *         arbitrary alphabets (as opposed to ISIN which generates decimal digits only)</li>
 *  </ul>
 */
public abstract class CrockfordBase32ChecksumValidator implements Predicate<String> {

    /**
     * How many check digits are added to payload. Currently, any values except 1 are not supported.
     */
    public static final int CHECKSUM_LENGTH = 1;

    /**
     * Boolean array for O(1) character validation. Each index represents a character's ASCII value (0..127): true =
     * valid character in {@link TextUtils#CROCKFORD_BASE32_ALPHABET}, false = invalid.
     *
     * @see #conformsToAlphabet(String)
     */
    private static final boolean[] IS_VALID_ALPHABET_CHAR = new boolean[128];

    static {
        for (char c : TextUtils.CROCKFORD_BASE32_ALPHABET.toCharArray()) {
            IS_VALID_ALPHABET_CHAR[c] = true;
        }
    }

    /**
     * Payload + checksum.
     */
    private final int minTotalLength;
    private final int maxTotalLength;

    @ConstraintArguments
    protected CrockfordBase32ChecksumValidator(int minPayloadLength, int maxPayloadLength) {
        check(maxPayloadLength, _CrockfordBase32ChecksumValidatorArgumentsMeta.MAXPAYLOADLENGTH.name())
                .positive();
        check(minPayloadLength, _CrockfordBase32ChecksumValidatorArgumentsMeta.MINPAYLOADLENGTH.name())
                .betweenInclusive(1, maxPayloadLength);
        minTotalLength = minPayloadLength + CHECKSUM_LENGTH;
        maxTotalLength = maxPayloadLength + CHECKSUM_LENGTH;

        // sanity check
        check(CHECKSUM_LENGTH, "checksum length").eq(1);
        check(TextUtils.CROCKFORD_BASE32_ALPHABET, "alphabet")
                .lengthBetweenInclusive(32, 32)
                .constraint(it -> it.chars().distinct().count() == it.length(), "must not contain duplicates");
    }

    @Override
    public boolean test(String id) {
        // put faster checks first
        return hasCorrectLength(id)
                && conformsToAlphabet(id)
                && TextUtils.notAllZeroes(id)
                && hasCorrectChecksum(id);
    }

    /**
     * Convenience method to throw an exception if the ID is invalid. The method is abstract because the exception
     * classes differ.
     *
     * @param id ID
     */
    public abstract void testWithException(String id);

    /**
     * Calculates and appends a check digit to the payload.
     *
     * @param payload payload (without a check digit)
     * @return payload with the check digit appended
     * @throws IllegalArgumentException if the check digit cannot be calculated, for example, if the input is
     *                                  {@code null} or an all-zero string
     */
    @ConstraintArguments
    public String addCheckDigit(String payload) {
        checkNotNull(payload, _CrockfordBase32ChecksumValidatorAddCheckDigitArgumentsMeta.PAYLOAD.name());
        return payload + calculateCheckDigit(payload);
    }

    private boolean hasCorrectLength(@Nullable String value) {
        if (value == null) {
            return false;
        }

        return (value.length() >= minTotalLength) && (value.length() <= maxTotalLength);
    }

    private boolean conformsToAlphabet(@Nullable String value) {
        if (value == null) {
            return false;
        }

        // this approach is said to be 2-5 times faster than regex matching
        for (int i = 0, n = value.length(); i < n; i++) {
            char c = value.charAt(i);

            if ((c >= IS_VALID_ALPHABET_CHAR.length) || !IS_VALID_ALPHABET_CHAR[c]) {
                return false;
            }
        }

        return true;
    }

    private boolean hasCorrectChecksum(@Nullable String value) {
        // no payload, or checksum is not a single character (because of charAt() below)
        if ((value == null) || (value.length() <= CHECKSUM_LENGTH) || (CHECKSUM_LENGTH != 1)) {
            return false;
        }

        String payload = value.substring(0, value.length() - CHECKSUM_LENGTH);
        char expectedCheckDigit = calculateCheckDigit(payload);
        char actualCheckDigit = value.charAt(value.length() - CHECKSUM_LENGTH);

        // faster than comparing strings
        return actualCheckDigit == expectedCheckDigit;
    }

    /**
     * Calculates a Damm check digit.
     *
     * @param payload input string
     * @return check digit, belongs to {@link TextUtils#CROCKFORD_BASE32_ALPHABET}
     * @throws IllegalStateException check digit calculation error
     */
    private char calculateCheckDigit(String payload) {
        return DammChecksumUtils.CROCKFORD_BASE32.compute(payload);
    }

}
