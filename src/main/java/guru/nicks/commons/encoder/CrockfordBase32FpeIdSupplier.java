package guru.nicks.commons.encoder;

import guru.nicks.commons.utils.crypto.FpeUtils;
import guru.nicks.commons.validation.CrockfordBase32ChecksumValidator;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Generates Crockford's Base32-encoded FPE-encrypted IDs using a combination of:
 * <ul>
 *  <li>a sequence number (supplier passed to constructor)</li>
 *  <li>encryption (parameters passed to constructor)</li>
 *  <li>Base32 {@link CrockfordBase32SequenceEncoder encoder}</li>
 * </ul>
 * The generated ID may contain leading zeroes but is never zeroes-only.
 * <p>
 * Original sequence numbers shorter than the value passed to constructor will be, before appending a check digit,
 * left-padded with zeroes (or its equivalent in the alphabet) in order to <b>hide the actual number of records</b>.
 * Here are a few estimates (big shops have around 350 million products and 1.5 billion orders a year):
 * <ul>
 *  <li>7 payload characters encode 34 billion records (32^7 = 34 359 738 368)</li>
 *  <li>6 payload characters encode 1 billion records (32^6 = 1 073 741 824)</li>
 *  <li>5 payload characters encode 33 million records (32^5 = 33 554 432)</li>
 *  <li>4 payload characters encode 1 million records (32^4 = 1 048 576)</li>
 * </ul>
 *
 * @see CrockfordBase32ChecksumValidator
 */
public abstract class CrockfordBase32FpeIdSupplier implements Supplier<String> {

    private final FpeUtils.SequenceEncryptor sequenceEncryptor;

    /**
     * Constructor.
     *
     * @param nextValueSupplier obtains next sequence value for ID
     * @param leftPadPositions  number of positions to which the sequence value will be left-padded (FF1 requires at
     *                          least a million variants for the input string, and since 10^6 = 1 000 000, this value
     *                          must be at least 6 for the decimal alphabet and at least 4 for Base32 because 32^4 = 1
     *                          048 576)
     * @param key               encryption key
     * @param tweak             tweak (like IV for AES)
     */
    protected CrockfordBase32FpeIdSupplier(LongSupplier nextValueSupplier, int leftPadPositions,
            byte[] key, byte[] tweak) {
        var base32Encoder = new CrockfordBase32SequenceEncoder();

        sequenceEncryptor = FpeUtils.createFf1SequenceEncryptor(
                () -> base32Encoder.encode(nextValueSupplier.getAsLong()),
                CrockfordBase32ChecksumValidator.ALPHABET,
                leftPadPositions, '0',
                key, tweak);
    }

    /**
     * Re-attempts generation until a not-all-zeroes string is obtained after encryption. Reason: modulo-based check
     * digits can't be calculated for all-zero input.
     * <p>
     * Subclasses should override this method to add a check digit and perform ID validation for safe-check.
     *
     * @return next encrypted ID
     */
    @Override
    public String get() {
        return sequenceEncryptor.getNextEncrypted();
    }

}
