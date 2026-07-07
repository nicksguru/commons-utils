package guru.nicks.commons.encoder;

import guru.nicks.commons.utils.crypto.FpeUtils;
import guru.nicks.commons.utils.text.TextUtils;
import guru.nicks.commons.validation.CrockfordBase32ChecksumValidator;

import am.ik.yavi.meta.ConstraintArguments;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Generates FPE-encrypted (FF3-1) values using a combination of:
 * <ul>
 *  <li>a sequence number (supplier passed to constructor)</li>
 *  <li>encryption (parameters passed to constructor)</li>
 *  <li>Crockford's Base32 {@link CrockfordBase32SequenceEncoder encoder}</li>
 * </ul>
 * The generated value may contain <b>leading zeroes</b> but is never zeroes-only.
 * <p>
 * Original sequence numbers shorter than the limit passed to the constructor are left-padded with zeroes - in order to
 * <b>hide the original number</b> (e.g., the number of shop orders). Here are a few estimates (big shops have around
 * 350 million products and 1.5 billion orders a year):
 * <ul>
 *  <li>1 character encodes numbers up to 31 (32^1 - 1 = 31 = z)</li>
 *  <li>2 characters encode numbers up to 1 thousand (32^2 - 1 = 1 023 = zz)</li>
 *  <li>3 characters encode numbers up to ≈32 thousand (32^3 - 1 = 32 767 = zzz)</li>
 *  <li>4 characters encode numbers up to ≈1 million (32^4 - 1 = 1 048 575 = zzzz)</li>
 *  <li>5 characters encode numbers up to ≈33 million (32^5 - 1 = 33 554 431 = zzzzz)</li>
 *  <li>6 characters encode numbers up to ≈1 billion (32^6 - 1 = 1 073 741 823 = zzzzzz)</li>
 *  <li>7 characters encode numbers up to ≈34 billion (32^7 - 1 = 34 359 738 367 = zzzzzzz)</li>
 * </ul>
 *
 * @see CrockfordBase32ChecksumValidator
 */
public abstract class CrockfordBase32FF31Supplier implements Supplier<String> {

    private final FpeUtils.SequenceEncryptor sequenceEncryptor;

    /**
     * Constructor. For arguments description see
     * {@link FpeUtils#createFf31SequenceEncryptor(Supplier, String, int, char, byte[], byte[])}.
     */
    @ConstraintArguments
    protected CrockfordBase32FF31Supplier(LongSupplier nextValueSupplier, int leftPadPositions,
            byte[] key, byte[] tweak) {
        checkNotNull(nextValueSupplier, _CrockfordBase32FF31SupplierArgumentsMeta.NEXTVALUESUPPLIER.name());
        var base32Encoder = new CrockfordBase32SequenceEncoder();

        sequenceEncryptor = FpeUtils.createFf31SequenceEncryptor(
                () -> base32Encoder.encode(nextValueSupplier.getAsLong()),
                TextUtils.CROCKFORD_BASE32_ALPHABET,
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
