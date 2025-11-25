package guru.nicks.commons.utils.crypto;

import guru.nicks.commons.utils.text.TextUtils;

import am.ik.yavi.meta.ConstraintArguments;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.fpe.FPEFF1Engine;
import org.bouncycastle.crypto.params.FPEParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * FPE (Format Preserving Encryption) utilities.
 */
@UtilityClass
public class FpeUtils {

    public static final String DECIMAL_ALPHABET = "0123456789";

    /**
     * Creates a sequence encryptor that uses the FF1 algorithm for format-preserving encryption. The result is never
     * all zero: if encryption yields an all-zero string, the next sequence value is obtained until the result is not
     * all-zero.
     *
     * @param nextValueSupplier supplier for the next sequence value (e.g., from a database sequence)
     * @param alphabet          alphabet, at least 10 characters (e.g. {@value #DECIMAL_ALPHABET} for integers)
     * @param leftPadPositions  number of positions to which the sequence value will be left-padded (FF1 demands at
     *                          least 6)
     * @param padCharacter      character to use for left-padding (e.g. '0' for integers)
     * @param key               encryption key (AES key size: 16 / 24 / 32 bytes)
     * @param tweak             tweak (AES IV)
     * @return a new {@link SequenceEncryptor} instance.
     */
    public SequenceEncryptor createFf1SequenceEncryptor(Supplier<String> nextValueSupplier, String alphabet,
            int leftPadPositions, char padCharacter,
            byte[] key, byte[] tweak) {
        return new Ff1SequenceEncryptor(nextValueSupplier, alphabet, leftPadPositions, padCharacter, key, tweak);
    }

    public interface SequenceEncryptor {

        /**
         * Obtains the next sequence value and encrypts it. Re-attempts the generation until a not all-zero string is
         * obtained after the encryption. Reason: modulo-based check digits can't be calculated for all-zero input.
         *
         * @return encrypted next sequence value
         * @throws IllegalStateException if - during all-zero mitigation - the next sequence value is the same as the
         *                               previous one (i.e. all-zero)
         */
        String getNextEncrypted();

        /**
         * Decrypts the sequence value previously encrypted with {@link #getNextEncrypted()}.
         * <p>
         * WARNING: the value returned is the one after padding, i.e. may be left-padded. For decimal numbers, use e.g.
         * {@link Long#parseLong(String)}.
         *
         * @param encryptedValue encrypted sequence value
         * @return decrypted sequence value <b>with padding included</b>
         * @throws IllegalArgumentException if the encrypted value is blank
         */
        String decrypt(String encryptedValue);

    }

    static class Ff1SequenceEncryptor implements SequenceEncryptor {

        /**
         * FF1 demands at least 1 000 000 variants of the input string; given the base of 10 (i.e. decimal digits),
         * `10^6 = 1 000 000`. Other alphabets may be OK with a smaller value, but then the minimum visual size comes
         * into effect (to hide the original sequence numbers), hence this global restriction.
         */
        private static final int MIN_LEFT_PAD_POSITIONS = 6;

        private final Supplier<String> nextValueSupplier;
        private final Function<String, String> sequenceValuePadder;
        private final UnaryOperator<String> encryptorFunction;
        private final UnaryOperator<String> decryptorFunction;
        private final String alphabet;
        private final char[] alphabetChars;

        /**
         * Constructor. For details, see
         * {@link #createFf1SequenceEncryptor(Supplier, String, int, char, byte[], byte[])}.
         */
        @ConstraintArguments
        Ff1SequenceEncryptor(Supplier<String> nextValueSupplier, String alphabet,
                int leftPadPositions, char padCharacter,
                byte[] key, byte[] tweak) {
            this.nextValueSupplier = checkNotNull(nextValueSupplier,
                    _FpeUtils_Ff1SequenceEncryptorArgumentsMeta.NEXTVALUESUPPLIER.name());

            check(alphabet, _FpeUtils_Ff1SequenceEncryptorArgumentsMeta.ALPHABET.name())
                    .lengthBetweenInclusive(10, 256);

            check(key, _FpeUtils_Ff1SequenceEncryptorArgumentsMeta.KEY.name())
                    .notNull()
                    .constraint(value -> (value.length == 16) || (value.length == 24) || (value.length == 32),
                            "size must be 16 / 24 / 32 bytes long");

            check(tweak, _FpeUtils_Ff1SequenceEncryptorArgumentsMeta.TWEAK.name())
                    .notNull()
                    .constraint(value -> value.length > 0, "must be non-empty");

            check(leftPadPositions, _FpeUtils_Ff1SequenceEncryptorArgumentsMeta.LEFTPADPOSITIONS.name())
                    .greaterThanOrEqual(MIN_LEFT_PAD_POSITIONS);

            check(padCharacter, _FpeUtils_Ff1SequenceEncryptorArgumentsMeta.PADCHARACTER.name())
                    .notNull()
                    .constraint(chr -> alphabet.contains(chr.toString()), "must be part of the alphabet");

            // can be chained with 'andThen', but each step needs to be debugged
            sequenceValuePadder = value -> StringUtils.leftPad(value, leftPadPositions, padCharacter);

            this.alphabet = alphabet;
            this.alphabetChars = alphabet.toCharArray();
            var fpeParams = new FPEParameters(new KeyParameter(key), alphabet.length(), tweak);

            var encryptEngine = new FPEFF1Engine();
            encryptEngine.init(true, fpeParams);

            // map characters from the custom alphabet to indexes in that alphabet (thus obtaining decimal values),
            // encrypt the decimal values,
            // convert the result (a decimal number too) back to custom alphabet
            encryptorFunction = input -> {
                byte[] plainText = alphabet2decimal(input);
                byte[] cipherText = new byte[plainText.length];
                encryptEngine.processBlock(plainText, 0, plainText.length, cipherText, 0);
                return decimal2alphabet(cipherText);
            };

            var decryptEngine = new FPEFF1Engine();
            decryptEngine.init(false, fpeParams);
            // same logic as for encryption
            decryptorFunction = input -> {
                byte[] cipherText = alphabet2decimal(input);
                byte[] plainText = new byte[cipherText.length];
                decryptEngine.processBlock(cipherText, 0, cipherText.length, plainText, 0);
                return decimal2alphabet(plainText);
            };
        }

        @Override
        public String getNextEncrypted() {
            String result;
            String prevSeqValue = null;

            do {
                String nextSeqValue = nextValueSupplier.get();
                checkNotBlank(nextSeqValue, "sequence value");

                // avoid eternal loop during all-zero workaround
                if (nextSeqValue.equals(prevSeqValue)) {
                    throw new IllegalStateException(
                            "Sequence value must be different from the previous one to mitigate all-zero result");
                }

                String paddedNextSeqValue = sequenceValuePadder.apply(nextSeqValue);

                result = encryptorFunction.apply(paddedNextSeqValue);
                prevSeqValue = nextSeqValue;
            } while (TextUtils.ALL_ZEROES_PREDICATE.test(result));

            return result;
        }

        @ConstraintArguments
        @Override
        public String decrypt(String encryptedValue) {
            checkNotBlank(encryptedValue, _FpeUtils_Ff1SequenceEncryptorDecryptArgumentsMeta.ENCRYPTEDVALUE.name());
            return decryptorFunction.apply(encryptedValue);

        }

        /**
         * Converts a string from the custom alphabet to an array of bytes representing the character indexes in it.
         * This is a necessary step before FPE encryption, which operates on numerical values only.
         *
         * @param input the string to convert
         * @return a byte array where each byte represents the index of the corresponding character in the alphabet
         * @throws IllegalArgumentException if the input string contains a character not found in the alphabet
         */
        private byte[] alphabet2decimal(String input) {
            byte[] bytes = new byte[input.length()];

            for (int i = 0; i < input.length(); i++) {
                int index = alphabet.indexOf(input.charAt(i));

                // -1 means not found; 255 is max. for a byte (see below)
                if ((index < 0) || (index > 255)) {
                    // don't reveal the character itself - it may be part of a password or another secret
                    throw new IllegalArgumentException("Input character is missing from the alphabet");
                }

                bytes[i] = (byte) index;
            }

            return bytes;
        }

        /**
         * Converts an array of bytes representing character indexes in the custom alphabet back to a string using the
         * alphabet. This is a necessary step after FPE decryption, which operates on numerical values only.
         *
         * @param input the byte array to convert, where each byte is an index in the alphabet
         * @return the resulting string
         * @throws IllegalArgumentException if any byte in the input, when treated as an unsigned value, is an invalid
         *                                  index for the alphabet
         */
        private String decimal2alphabet(byte[] input) {
            var builder = new StringBuilder(input.length);

            for (byte b : input) {
                try {
                    // byte is signed in Java, so it can be negative.
                    // `b & 0xFF` converts it to an int in the range [0, 255] before using it as an index.
                    builder.append(alphabetChars[b & 0xFF]);
                } catch (IndexOutOfBoundsException e) {
                    throw new IllegalArgumentException("Input character is missing from the alphabet", e);
                }
            }

            return builder.toString();
        }

    }

}
