package guru.nicks.commons.utils.crypto;

import am.ik.yavi.meta.ConstraintArguments;
import lombok.experimental.UtilityClass;
import ubiqsecurity.fpe.FF1;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * FPE (Format Preserving Encryption) utilities.
 */
@UtilityClass
public class FpeUtils {

    /**
     * Creates a sequence encryptor that uses the FF1 algorithm for format-preserving encryption. The result is never
     * all zero: if encryption yields an all-zero string, the next sequence value is obtained until the result is not
     * all-zero.
     *
     * @param nextValueSupplier  supplier for the next sequence value (e.g., from a database sequence)
     * @param zeroPadValueDigits number of digits to which the sequence value will be zero-padded (at least 6)
     * @param key                encryption key (AES key size: 16 / 24 / 32 bytes)
     * @param tweak              tweak (AES IV)
     * @return a new {@link SequenceEncryptor} instance.
     */
    public SequenceEncryptor createFf1SequenceEncryptor(Supplier<Long> nextValueSupplier, int zeroPadValueDigits,
            byte[] key, byte[] tweak) {
        return new Ff1SequenceEncryptor(nextValueSupplier, zeroPadValueDigits, key, tweak);
    }

    public interface SequenceEncryptor {

        Predicate<String> ALL_ZERO_PREDICATE = Pattern.compile("^0+$").asMatchPredicate();

        /**
         * Obtains the next sequence value and encrypts it. Re-attempts the generation until a not all-zero string is
         * obtained after the encryption. Reason: modulo-based check digits can't be calculated for all-zero input.
         *
         * @return encrypted next sequence value
         * @throws IllegalStateException if sequence value is not positive or - during all-zero mitigation - the next
         *                               sequence value is the same as the previous one
         */
        String getNext();

        /**
         * Decrypts the sequence value previously encrypted with {@link #getNext()}.
         *
         * @param encryptedValue encrypted sequence value
         * @return decrypted sequence value
         * @throws IllegalArgumentException if the encrypted value can't be converted to a {@code long} (as experiments
         *                                  show, passing non-numeric values creates a valid  {@code long} anyway)
         */
        long decrypt(String encryptedValue);

    }

    public static class Ff1SequenceEncryptor implements SequenceEncryptor {

        private static final int MIN_ZERO_PAD_DIGITS = 6;

        private final FF1 fpe;

        /**
         * Left-pad with zeroes to at least {@value #MIN_ZERO_PAD_DIGITS} decimal positions. FF1 demands at least 1 000
         * 000 variants of the input string; given the base of 10 (i.e. decimal digits), 10^6 = 1 000 000.
         */
        private final String zeroPadValueDigitsTemplate;

        private final Supplier<Long> sequenceValueSupplier;
        private final Function<Long, String> sequenceValuePadder;
        private final UnaryOperator<String> encryptorFunction;

        /**
         * Constructor. For details, see {@link #createFf1SequenceEncryptor(Supplier, int, byte[], byte[])}.
         */
        @ConstraintArguments
        Ff1SequenceEncryptor(Supplier<Long> nextValueSupplier, int zeroPadValueDigits, byte[] key, byte[] tweak) {
            this.sequenceValueSupplier = checkNotNull(nextValueSupplier,
                    _FpeUtils_Ff1SequenceEncryptorArgumentsMeta.NEXTVALUESUPPLIER.name());
            check(key, _FpeUtils_Ff1SequenceEncryptorArgumentsMeta.KEY.name())
                    .notNull()
                    .constraint(value -> (value.length == 16) || (value.length == 24) || (value.length == 32),
                            "size must be 16 / 24 / 32 bytes long");
            checkNotNull(tweak, _FpeUtils_Ff1SequenceEncryptorArgumentsMeta.TWEAK.name());

            // see PADDED_INPUT comment
            check(zeroPadValueDigits, _FpeUtils_Ff1SequenceEncryptorArgumentsMeta.ZEROPADVALUEDIGITS.name())
                    .greaterThanOrEqual(MIN_ZERO_PAD_DIGITS);
            zeroPadValueDigitsTemplate = "%0" + zeroPadValueDigits + "d";

            // can be chained with 'andThen', but each step needs to be debugged
            sequenceValuePadder = value -> String.format(Locale.US, zeroPadValueDigitsTemplate, value);

            // WARNING: despite the explicit decimal alphabet, decryption of non-numeric strings creates valid numbers!
            fpe = new FF1(key, tweak, 0, 0, 10, "0123456789");
            encryptorFunction = fpe::encrypt;
        }

        @Override
        public String getNext() {
            String result;
            Long prevSeqValue = null;

            do {
                Long nextSeqValue = sequenceValueSupplier.get();
                // sanity check
                if ((nextSeqValue == null) || (nextSeqValue <= 0)) {
                    throw new IllegalStateException("Sequence value must be positive");
                }

                // avoid eternal loop during all-zero workaround
                if (nextSeqValue.equals(prevSeqValue)) {
                    throw new IllegalStateException(
                            "Sequence value must be different from the previous one to mitigate all-zero result");
                }

                String paddedNextSeqValue = sequenceValuePadder.apply(nextSeqValue);
                result = encryptorFunction.apply(paddedNextSeqValue);
                prevSeqValue = nextSeqValue;
            } while (ALL_ZERO_PREDICATE.test(result));

            return result;
        }

        @Override
        public long decrypt(String encryptedValue) {
            try {
                return Long.parseLong(
                        fpe.decrypt(encryptedValue));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Decrypted value is not a valid long: " + e.getMessage(), e);
            }
        }

    }

}
