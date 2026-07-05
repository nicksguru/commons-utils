package guru.nicks.commons.validation;

import guru.nicks.commons.encoder.CrockfordBase32FpeIdSupplier;
import guru.nicks.commons.utils.text.TextUtils;

import am.ik.yavi.meta.ConstraintArguments;
import jakarta.annotation.Nullable;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.ISINCheckDigit;

import java.util.function.Predicate;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Validates input with respect to:
 * <ul>
 *     <li>min. length (passed to constructor)</li>
 *     <li>max. length ({@link #MAX_TOTAL_LENGTH} resulting from encoding {@link Long#MAX_VALUE} and appending a
 *         check digit)</li>
 *     <li>payload content ({@link #ALPHABET})</li>
 *     <li>check digit calculated using the ISIN algorithm (possibly altered by
 *         {@link #convertCheckDigitToIndexInAlphabet(int)}) which, like all module-based ones, fails on all-zero
 *         input - therefore all-zero strings are always considered invalid</li>
 *  </ul>
 *
 * @see CrockfordBase32FpeIdSupplier
 */
public abstract class CrockfordBase32ChecksumValidator implements Predicate<String> {

    /**
     * Crockford's Base32 character set.
     */
    public static final String ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz";

    /**
     * {@link Long#MAX_VALUE} is encoded as {@code 7zzzzzzzzzzzz} - 13 characters.
     */
    public static final int MAX_PAYLOAD_LENGTH = 13;

    /**
     * How many check digits are added to payload. Currently, any values except 1 are not supported.
     */
    public static final int CHECKSUM_LENGTH = 1;

    /**
     * {@link #MAX_PAYLOAD_LENGTH} + {@link #CHECKSUM_LENGTH}.
     */
    public static final int MAX_TOTAL_LENGTH = MAX_PAYLOAD_LENGTH + CHECKSUM_LENGTH;

    /**
     * Boolean array for O(1) character validation. Each index represents a character's ASCII value (0..127): true =
     * valid character in {@link #ALPHABET}, false = invalid.
     *
     * @see #conformsToAlphabet(String)
     */
    private static final boolean[] IS_VALID_ALPHABET_CHAR = new boolean[128];

    static {
        for (char c : ALPHABET.toCharArray()) {
            IS_VALID_ALPHABET_CHAR[c] = true;
        }
    }

    /**
     * Payload + checksum.
     */
    private final int minTotalLength;

    @ConstraintArguments
    protected CrockfordBase32ChecksumValidator(int minPayloadLength) {
        check(minPayloadLength, _CrockfordBase32ChecksumValidatorArgumentsMeta.MINPAYLOADLENGTH.name())
                .betweenInclusive(1, MAX_PAYLOAD_LENGTH);
        minTotalLength = minPayloadLength + CHECKSUM_LENGTH;

        // sanity check
        check(CHECKSUM_LENGTH, "checksum length").eq(1);
        check(ALPHABET, "alphabet")
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

    /**
     * Post-processes the check digit if needed.
     *
     * @param checkDigit check digit (0..9)
     * @return index in {@link #ALPHABET}
     */
    protected abstract int convertCheckDigitToIndexInAlphabet(int checkDigit);

    private boolean hasCorrectLength(@Nullable String value) {
        if (value == null) {
            return false;
        }

        return (value.length() >= minTotalLength) && (value.length() <= MAX_TOTAL_LENGTH);
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
        // no payload, or checksum is not a single character
        if ((value == null) || (value.length() <= CHECKSUM_LENGTH) || (CHECKSUM_LENGTH != 1)) {
            return false;
        }

        String payload = value.substring(0, value.length() - CHECKSUM_LENGTH);
        char expectedCheckDigit = calculateCheckDigit(payload);
        char actualCheckDigit = value.charAt(value.length() - CHECKSUM_LENGTH);

        return actualCheckDigit == expectedCheckDigit;
    }

    /**
     * Calculates an ISIN check digit, then maps it onto {@link #ALPHABET} with
     * {@link #convertCheckDigitToIndexInAlphabet(int)}.
     *
     * @param value input string
     * @return check digit, belongs to {@link #ALPHABET}
     * @throws IllegalStateException check digit calculation error
     */
    private char calculateCheckDigit(String value) {
        int checkDigit;

        try {
            // By the ISIN algorithm definition, the result is a single decimal character: '0' to '9'.
            // WARNING: like all modulo-based algorithms, it throws an exception on all-zero input.
            String rawCheckDigit = ISINCheckDigit.ISIN_CHECK_DIGIT.calculate(value);
            checkDigit = Integer.parseInt(rawCheckDigit);

            if ((checkDigit < 0) || (checkDigit > 9)) {
                throw new IllegalArgumentException("Check digit is not decimal");
            }
        } catch (CheckDigitException e) {
            throw new IllegalArgumentException("Check digit calculation error: " + e.getMessage(), e);
        }

        int indexInAlphabet = convertCheckDigitToIndexInAlphabet(checkDigit);
        return ALPHABET.charAt(indexInAlphabet);
    }

}
