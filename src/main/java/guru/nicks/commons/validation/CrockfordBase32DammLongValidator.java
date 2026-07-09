package guru.nicks.commons.validation;

import guru.nicks.commons.utils.crypto.DammChecksumUtils;

import java.util.function.Function;

/**
 * Validates {@link Long} ID content, min/max length, and a {@link DammChecksumUtils#CROCKFORD_BASE32} check digit.
 *
 */
public abstract class CrockfordBase32DammLongValidator extends CrockfordBase32ChecksumValidator {

    /**
     * This value doesn't include the check digit. {@link Long#MAX_VALUE} is encoded as {@code 7ZZZZZZZZZZZZ}.
     */
    public static final int MAX_PAYLOAD_LENGTH = 13;

    private final Function<String, RuntimeException> exceptionFactory;

    /**
     * Constructor.
     *
     * @param minPayloadLength minimum payload length (not including the check digit)
     * @param exceptionFactory exception constructor accepting the invalid ID; the exception isn't obliged to store the
     *                         original message, but it should accept it
     */
    protected CrockfordBase32DammLongValidator(int minPayloadLength,
            Function<String, RuntimeException> exceptionFactory) {
        super(minPayloadLength, MAX_PAYLOAD_LENGTH);

        // give it a try to fail-fast in case of errors e.g. returning null
        RuntimeException e = exceptionFactory.apply("testing exception factory");
        if (e == null) {
            throw new IllegalArgumentException("Exception factory must return RuntimeException");
        }

        this.exceptionFactory = exceptionFactory;
    }

    /**
     * Convenience method to throw an exception (passed to constructor) if the given ID is invalid.
     *
     * @param id ID
     */
    @Override
    public void testWithException(String id) {
        if (!test(id)) {
            throw exceptionFactory.apply(id);
        }
    }

    /**
     * Calculates Damm checksum check digit for the payload. Throws some kind of {@link RuntimeException} if the
     * calculation fails e.g. due to null/invalid payload.
     *
     * @param payload input string without check digit
     * @return check digit from Crockford Base32 alphabet
     */
    @Override
    protected char calculateCheckDigit(String payload) {
        return DammChecksumUtils.CROCKFORD_BASE32.compute(payload);
    }

}
