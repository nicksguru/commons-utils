package guru.nicks.commons.utils.crypto;

import guru.nicks.commons.utils.text.TextUtils;

import am.ik.yavi.meta.ConstraintArguments;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.fpe.FPEEngine;
import org.bouncycastle.crypto.fpe.FPEFF1Engine;
import org.bouncycastle.crypto.fpe.FPEFF3_1Engine;
import org.bouncycastle.crypto.params.FPEParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * FPE (Format Preserving Encryption) utilities.
 */
@UtilityClass
public class FpeUtils {

    /**
     * Creates a sequence encryptor that uses the FF3-1 algorithm for format-preserving encryption. This algorithm (and
     * not FF1) is used by Visa and other major players because it calls AES only once in each round (FF1 calls AES
     * multiple times in each round which is more secure, but too slow).
     * <p>
     * The result is never all zero: if encryption yields an all-zero string, the next sequence value is obtained until
     * the result is not all-zero.
     *
     * @param nextValueSupplier supplier for the next sequence value (e.g., from a database sequence)
     * @param alphabet          alphabet, from 10 to 256 characters (e.g. {@link TextUtils#DECIMAL_ALPHABET} for
     *                          integers), must not contain duplicates
     * @param leftPadPositions  number of positions to which the sequence value will be left-padded, as NIST recommends
     *                          at least a million variants for the input string for even distribution; since 10^6 = 1
     *                          000 000, this value must be at least 6 for the decimal alphabet and at least 4 for
     *                          Base32 (because 32^4 = 1 048 576)
     * @param padCharacter      character to use for left-padding ('0' for integers)
     * @param key               AES key (128/192/256 bits)
     * @param tweak             AES tweak (56 bits = 7 bytes)
     * @return a new {@link SequenceEncryptor} instance
     */
    public SequenceEncryptor createFf31SequenceEncryptor(Supplier<String> nextValueSupplier, String alphabet,
            int leftPadPositions, char padCharacter,
            byte[] key, byte[] tweak) {
        return new FpeSequenceEncryptor(nextValueSupplier, alphabet, leftPadPositions, padCharacter,
                FPEFF3_1Engine::new, key, tweak);
    }

    /**
     * Same as {@link #createFf31SequenceEncryptor(Supplier, String, int, char, byte[], byte[])}, but leverages the FF1
     * algorithm. It's more secure, but excessively slower, and is here mostly for backward compatibility.
     */
    public SequenceEncryptor createFf1SequenceEncryptor(Supplier<String> nextValueSupplier, String alphabet,
            int leftPadPositions, char padCharacter,
            byte[] key, byte[] tweak) {
        return new FpeSequenceEncryptor(nextValueSupplier, alphabet, leftPadPositions, padCharacter,
                FPEFF1Engine::new, key, tweak);
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

    static class FpeSequenceEncryptor implements SequenceEncryptor {

        /**
         * FF31 requires at least a million variants of the input string for even distribution; given the base of 10
         * (decimal digits), 10^6 = 1 000 000, therefore at least 6 positions are needed. Other alphabets may be OK with
         * a smaller value, for example, for Base32 it's 4 because 32^4 = 1 048 576.
         */
        private static final int MIN_LEFT_PAD_POSITIONS = 4;

        private final Supplier<String> nextValueSupplier;
        private final Function<String, String> sequenceValuePadder;
        private final UnaryOperator<String> encryptorFunction;
        private final UnaryOperator<String> decryptorFunction;

        /**
         * Each value is the index of the corresponding character (the key) in the alphabet.
         */
        private final Map<Character, Integer> alphabetIndexMap;

        private final char[] alphabetChars;

        /**
         * Constructor. For details, see
         * {@link #createFf31SequenceEncryptor(Supplier, String, int, char, byte[], byte[])}.
         */
        @ConstraintArguments
        FpeSequenceEncryptor(Supplier<String> nextValueSupplier, String alphabet,
                int leftPadPositions, char padCharacter,
                Supplier<FPEEngine> fpeEngineSupplier, byte[] aesKey, byte[] aesTweak) {
            this.nextValueSupplier = checkNotNull(nextValueSupplier,
                    _FpeUtils_FpeSequenceEncryptorArgumentsMeta.NEXTVALUESUPPLIER.name());

            check(alphabet, _FpeUtils_FpeSequenceEncryptorArgumentsMeta.ALPHABET.name())
                    .lengthBetweenInclusive(10, 256)
                    .constraint(str -> str.chars().distinct().count() == str.length(), "has duplicate characters");

            // if alphabet is decimal-only, min. payload length is 6
            if (alphabet.chars().allMatch(chr -> (chr >= '0' && chr <= '9'))) {
                check(leftPadPositions, _FpeUtils_FpeSequenceEncryptorArgumentsMeta.LEFTPADPOSITIONS.name())
                        .constraint(it -> it >= 6,
                                "must be 6 or more - NIST recommends at least a million variants of input");
            } else {
                check(leftPadPositions, _FpeUtils_FpeSequenceEncryptorArgumentsMeta.LEFTPADPOSITIONS.name())
                        .constraint(it -> it >= MIN_LEFT_PAD_POSITIONS, "must be "
                                + MIN_LEFT_PAD_POSITIONS
                                + " or more - NIST recommends at least a million variants of input");
            }

            check(padCharacter, _FpeUtils_FpeSequenceEncryptorArgumentsMeta.PADCHARACTER.name())
                    .notNull()
                    .constraint(chr -> alphabet.contains(chr.toString()), "must be part of the alphabet");

            // can be chained with 'andThen', but each step needs to be debuggable
            sequenceValuePadder = value -> StringUtils.leftPad(value, leftPadPositions, padCharacter);

            // store characters fast lookup in decimal2alphabet()
            this.alphabetChars = alphabet.toCharArray();
            // store character indexes for fast lookup in alphabet2decimal()
            alphabetIndexMap = IntStream.range(0, alphabet.length())
                    .boxed()
                    .collect(Collectors.toMap(alphabet::charAt, index -> index));

            var fpeParams = new FPEParameters(new KeyParameter(aesKey), alphabet.length(), aesTweak);
            // the engine checks key size and tweak size - it'd be superfluous to check it here
            var encryptEngine = fpeEngineSupplier.get();
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

            FPEEngine decryptEngine = fpeEngineSupplier.get();
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
            } while (!TextUtils.notAllZeroes(result));

            return result;
        }

        @ConstraintArguments
        @Override
        public String decrypt(String encryptedValue) {
            checkNotBlank(encryptedValue, _FpeUtils_FpeSequenceEncryptorDecryptArgumentsMeta.ENCRYPTEDVALUE.name());
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
                Integer index = alphabetIndexMap.get(input.charAt(i));

                // null means not found; 255 is max. for a byte (see below)
                if ((index == null) || (index > 255)) {
                    // don't reveal the character itself - it may be part of a password or another secret
                    throw new IllegalArgumentException("Input character is missing from the alphabet");
                }

                bytes[i] = index.byteValue();
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
                    // byte is signed in Java, so it can be negative;
                    // `b & 0xFF` converts it to an int in the range [0, 255] before using it as an index
                    builder.append(alphabetChars[b & 0xFF]);
                }
                // this can happen if the encrypted value was created with a different alphabet.
                catch (IndexOutOfBoundsException e) {
                    throw new IllegalArgumentException("Invalid character index found after decryption", e);
                }
            }

            return builder.toString();
        }

    }

}
