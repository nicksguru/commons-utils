package guru.nicks.utils;

import com.google.common.primitives.Longs;
import net.openhft.hashing.LongHashFunction;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.ISINCheckDigit;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.apache.commons.validator.routines.checkdigit.VerhoeffCheckDigit;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.digests.ShortenedDigest;

import java.nio.charset.StandardCharsets;

import static guru.nicks.validation.dsl.ValiDsl.check;

public enum HashUtils {

    /**
     * XxHash3 is very fast and has a very good randomness and dispersion, but it's not a crypto-grade function, which
     * means: knowing a hash value, it's possible to <b>algorithmically (without brute force)</b> generate such input
     * that yields the same hash. That input may or may not be the original one, but the hash value is the same.
     * <p>
     * NOTE: transforming the hash value to its native {@link Long} representation with, for example,
     * {@link Longs#fromByteArray(byte[])} may produce a negative value.
     */
    XXHASH3 {
        /**
         * @return 8
         */
        @Override
        public int getMaxHashLengthBytes() {
            return Long.BYTES;
        }

        @Override
        protected byte[] computeInternal(byte[] source, int hashLengthBytes) {
            byte[] hash = Longs.toByteArray(
                    LongHashFunction.xx3().hashBytes(source));

            // complete hash was requested
            if (hashLengthBytes == hash.length) {
                return hash;
            }

            return ArrayUtils.subarray(hash, 0, hashLengthBytes);
        }
    },

    /**
     * SHA-256 is a crypto-grade algorithm with a fixed hash length.
     */
    SHA_256 {
        /**
         * @return 32
         */
        @Override
        public int getMaxHashLengthBytes() {
            return 256 / Byte.SIZE;
        }

        @Override
        protected byte[] computeInternal(byte[] source, int hashLengthBytes) {
            return DigestUtils.sha256(source);
        }
    },

    /**
     * SHA3 is a crypto-grade algorithm with a variable hash length.
     * <p>
     * SHA3-512 is always used regardless of the desired hash length, as SHA3-256 yields totally different results (it's
     * not a prefix to SHA3-512). It's desirable to use and truncate the same hash, for consistency.
     */
    SHA3_512 {
        /**
         * @return 64
         */
        @Override
        public int getMaxHashLengthBytes() {
            return 512 / Byte.SIZE;
        }

        @Override
        protected void checkRequestedHashLength(int hashLengthBytes) {
            check(hashLengthBytes, "hash length").betweenInclusive(1, getMaxHashLengthBytes());
        }

        @Override
        protected byte[] computeInternal(byte[] source, int hashLengthBytes) {
            byte[] target = new byte[hashLengthBytes];

            // always use SHA3-512 for consistency, as SHA-256 results differ from SHA3-512 significantly
            var digest = new ShortenedDigest(new SHA3Digest(512), target.length);
            digest.update(source, 0, source.length);
            digest.doFinal(target, 0);

            return target;
        }
    },

    /**
     * Calculates the Luhn check digit (integer [0-9]) for the given decimal string, such as a credit card number.
     * crashes on all-zero input.
     * <p>
     * Throws {@link IllegalArgumentException} if the input is blank, or all-zero, or not numeric. The all-zero check is
     * common to all modulo-based algorithms, see {@code ModulusCheckDigit#calculateModulus(String, boolean)}).
     */
    LUHN_DIGIT {
        /**
         * @return 1
         */
        @Override
        public int getMaxHashLengthBytes() {
            return 1;
        }

        /**
         * @return bytes of a string holding a decimal digit
         * @throws IllegalArgumentException the input is blank, or all-zero, or not numeric
         */
        @Override
        protected byte[] computeInternal(byte[] source, int hashLengthBytes) {
            String str;

            try {
                str = LuhnCheckDigit.LUHN_CHECK_DIGIT.calculate(
                        new String(source, StandardCharsets.UTF_8));
            } catch (CheckDigitException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }

            return str.getBytes(StandardCharsets.UTF_8);
        }
    },

    /**
     * Calculates the ISIN check digit (integer [0-9]) for the given [A-Za-z0-9] string. This is an extension to the
     * Luhn algorithm where A=0, B=1, ..., Z=35.
     * <p>
     * WARNING: the Apache Commons implementation treats lowercase characters like uppercase (because it calls
     * {@link Character#getNumericValue(char)})!
     * <p>
     * Throws {@link IllegalArgumentException} if the input is blank, or all-zero, or contains non-alphanumeric
     * characters. The all-zero check is common to all modulo-based algorithms, see
     * {@code ModulusCheckDigit#calculateModulus(String, boolean)}).
     */
    ISIN_DIGIT {
        /**
         * @return 1
         */
        @Override
        public int getMaxHashLengthBytes() {
            return 1;
        }

        /**
         * @return bytes of a string holding a decimal digit
         * @throws IllegalArgumentException the input is blank, or all-zero, or contains non-alphanumeric characters
         */
        @Override
        protected byte[] computeInternal(byte[] source, int hashLengthBytes) {
            String str;

            try {
                str = ISINCheckDigit.ISIN_CHECK_DIGIT.calculate(
                        new String(source, StandardCharsets.UTF_8));
            } catch (CheckDigitException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }

            return str.getBytes(StandardCharsets.UTF_8);
        }
    },

    /**
     * Calculates the Verhoeff check digit (integer [0..9]) for the given decimal string. Unlike modulo-based
     * algorithms, tolerates all-zero input. Catches more errors than the Luhn algorithm. See description <a
     * href="https://en.wikipedia.org/wiki/Verhoeff_algorithm">here</a>.
     * <p>
     * Throws {@link IllegalArgumentException} if the input is blank or non-decimal.
     */
    VERHOEFF {
        /**
         * @return 1
         */
        @Override
        public int getMaxHashLengthBytes() {
            return 1;
        }

        /**
         * @return bytes of a string holding a decimal digit
         * @throws IllegalArgumentException the input is blank or non-decimal
         */
        @Override
        protected byte[] computeInternal(byte[] source, int hashLengthBytes) {
            String str;

            try {
                str = VerhoeffCheckDigit.VERHOEFF_CHECK_DIGIT.calculate(
                        new String(source, StandardCharsets.UTF_8));
            } catch (CheckDigitException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }

            return str.getBytes(StandardCharsets.UTF_8);
        }
    };

    /**
     * Computes hash of default length.
     *
     * @param source source data
     * @return hash of length {@link #getDefaultHashLengthBytes()}
     */
    public byte[] compute(byte[] source) {
        return compute(source, getDefaultHashLengthBytes());
    }

    /**
     * Computes hash of the given length.
     *
     * @param source          source data
     * @param hashLengthBytes requested hash length, will be verified by {@link #checkRequestedHashLength(int)}
     * @return hash of the requested length
     * @throws IllegalArgumentException requested hash length is unsupported
     */
    public byte[] compute(byte[] source, int hashLengthBytes) {
        checkRequestedHashLength(hashLengthBytes);
        return computeInternal(source, hashLengthBytes);
    }

    /**
     * Calls {@link #compute(byte[])} and encodes the result as a lowercase hex string.
     *
     * @param source source data
     * @return hash of the requested length encoded as a lowercase hex string
     * @throws IllegalArgumentException requested hash length is unsupported
     */
    public String computeHex(byte[] source) {
        return Hex.encodeHexString(compute(source));
    }

    /**
     * Calls {@link #compute(byte[], int)} and encodes the result as a lowercase hex string.
     *
     * @param source          source data
     * @param hashLengthBytes requested hash length, will be verified by {@link #checkRequestedHashLength(int)}
     * @return hash of the requested length encoded as a lowercase hex string
     * @throws IllegalArgumentException requested hash length is unsupported
     */
    public String computeHex(byte[] source, int hashLengthBytes) {
        return Hex.encodeHexString(compute(source, hashLengthBytes));
    }

    /**
     * Called from {@link #compute(byte[], int)} after {@link #checkRequestedHashLength(int)} has been called.
     *
     * @param hashLengthBytes requested hash length
     * @return hash of the requested length
     */
    protected abstract byte[] computeInternal(byte[] source, int hashLengthBytes);

    /**
     * @return max. hash length in bytes
     */
    public abstract int getMaxHashLengthBytes();

    /**
     * Returns default hash length in bytes.
     *
     * @return default implementation returns {@link #getMaxHashLengthBytes()}
     */
    public int getDefaultHashLengthBytes() {
        return getMaxHashLengthBytes();
    }

    /**
     * Default implementation checks for {@link #getMaxHashLengthBytes()} equality because most hash functions generate
     * a fixed length hash.
     *
     * @param hashLengthBytes requested hash length
     * @throws IllegalArgumentException requested hash length out of range
     */
    protected void checkRequestedHashLength(int hashLengthBytes) {
        check(hashLengthBytes, "hash length").betweenInclusive(
                getMaxHashLengthBytes(), getMaxHashLengthBytes());
    }

}
