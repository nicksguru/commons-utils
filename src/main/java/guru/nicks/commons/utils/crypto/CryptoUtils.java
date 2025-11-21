package guru.nicks.commons.utils.crypto;

import am.ik.yavi.meta.ConstraintArguments;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Encryption/decryption utilities.
 */
@UtilityClass
public class CryptoUtils {

    /**
     * Needed for {@link #rsaEncrypt(byte[], PublicKey)}.
     */
    private static final String RSA_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /**
     * 'AES' stands for 'AES-256'.
     */
    private static final String AES_ALGORITHM = "AES";

    /**
     * Don't use ECB (the default) or CBC.
     */
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";

    /**
     * IV length for AES. For GCM, the standard is 12.
     */
    private static final int AES_IV_LENGTH_BYTES = 12;

    /**
     * Salt length for PBKDF2 key derivation. 16 bytes is a good choice.
     */
    private static final int AES_SALT_LENGTH_BYTES = 16;

    /**
     * AES key length in bits. 256 is the strongest.
     */
    private static final int AES_KEY_LENGTH_BITS = 256;

    /**
     * Iteration count for PBKDF2. Higher is more secure but slower. OWASP recommends at least 600,000 for
     * PBKDF2-HMAC-SHA256.
     */
    private static final int PBKDF2_ITERATIONS = 600_000;

    /**
     * Algorithm for deriving a key from a password/secret.
     */
    private static final String SECRET_KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * GCM authentication tag length in bits. 128 is standard.
     */
    private static final int GCM_TAG_LENGTH_BITS = 128;

    /**
     * Secure random number generator. Needed for random IV generation for each message being encrypted.
     */
    private final SecureRandom random = new SecureRandom();

    /**
     * Encodes bytes as Base64.
     *
     * @param input bytes to encode
     * @return Base64-encoded string (with trailing '=' if needed)
     */
    public String encodeBase64(byte[] input) {
        return Base64.getEncoder()
                .encodeToString(input);
    }

    /**
     * Decodes a Base64 string.
     *
     * @param encoded string to encode
     * @return bytes
     */
    public byte[] decodeBase64(String encoded) {
        return Base64.getDecoder()
                .decode(encoded.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Calculates HMAC SHA-512.
     *
     * @param secretKey secret key
     * @param input     text to calculate HMAC for
     * @return HMAC
     */
    public byte[] calculateHmacSha512(byte[] input, String secretKey) {
        return HmacUtils
                .getInitializedMac(HmacAlgorithms.HMAC_SHA_512, secretKey.getBytes(StandardCharsets.UTF_8))
                .doFinal(input);
    }

    /**
     * RSA-encrypts plain text.
     *
     * @param plainText text to encrypt
     * @param publicKey public RSA key
     * @return bytes encrypted
     */
    @SneakyThrows(GeneralSecurityException.class)
    @ConstraintArguments
    public byte[] rsaEncrypt(byte[] plainText, PublicKey publicKey) {
        checkNotNull(publicKey, _CryptoUtilsRsaEncryptArgumentsMeta.PUBLICKEY.name());
        checkNotNull(plainText, _CryptoUtilsRsaEncryptArgumentsMeta.PLAINTEXT.name());

        var cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(plainText);
    }

    /**
     * Decrypts RSA-encrypted text.
     *
     * @param encrypted  bytes to encrypt (passing garbage will cause the method to throw an exception)
     * @param privateKey private RSA key
     * @return bytes decrypted
     */
    @SneakyThrows(GeneralSecurityException.class)
    @ConstraintArguments
    public byte[] rsaDecrypt(byte[] encrypted, PrivateKey privateKey) {
        checkNotNull(encrypted, _CryptoUtilsRsaDecryptArgumentsMeta.ENCRYPTED.name());
        checkNotNull(encrypted, _CryptoUtilsRsaDecryptArgumentsMeta.PRIVATEKEY.name());

        var cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encrypted);
    }

    /**
     * AES-encrypts plain text. The result has a certain structure which only {@link #aesDecrypt(byte[], String)}
     * understands.
     *
     * @param plainText plain text
     * @param secretKey secret key
     * @return encrypted text
     */
    @SneakyThrows(GeneralSecurityException.class)
    @ConstraintArguments
    public byte[] aesEncrypt(byte[] plainText, String secretKey) {
        checkNotNull(plainText, _CryptoUtilsAesEncryptArgumentsMeta.PLAINTEXT.name());
        check(secretKey, _CryptoUtilsAesEncryptArgumentsMeta.SECRETKEY.name()).notEmpty();
        check(AES_TRANSFORMATION, "AES mode")
                .notBlank()
                .constraint(aesMode -> aesMode.contains("/GCM/"), "must be in GCM mode: ECB/CBC are vulnerable");

        // generate random salt for key derivation
        var salt = new byte[AES_SALT_LENGTH_BYTES];
        random.nextBytes(salt);

        // generate a random IV - to make sure identical messages never look the same after encryption
        var iv = new byte[AES_IV_LENGTH_BYTES];
        random.nextBytes(iv);

        // derive a secure key from the user-provided secret and salt
        SecretKey aesKey = deriveAesKey(secretKey, salt);
        // authentication tag preventing producing garbage output from garbage input
        var gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

        var cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        byte[] encrypted = cipher.doFinal(plainText);

        // Prepend salt and IV (they are not secret) to encrypted text. Format: [salt | iv | ciphertext].
        byte[] saltAndIv = ArrayUtils.addAll(salt, iv);
        return ArrayUtils.addAll(saltAndIv, encrypted);
    }

    /**
     * Decrypts AES-encrypted text (the result of {@link #aesEncrypt(byte[], String)} - it has a certain structure).
     *
     * @param secretKey secret key
     * @param encrypted text to decrypt (passing garbage will cause the method to throw an exception)
     * @return text decrypted
     * @throws IllegalArgumentException wrong input text structure
     */
    @SuppressWarnings("java:S3329") // allow use of non-random IV (Sonar doesn't realize this is DECRYPTION)
    @SneakyThrows(GeneralSecurityException.class)
    @ConstraintArguments
    public byte[] aesDecrypt(byte[] encrypted, String secretKey) {
        checkNotNull(encrypted, _CryptoUtilsAesDecryptArgumentsMeta.ENCRYPTED.name());
        check(secretKey, _CryptoUtilsAesDecryptArgumentsMeta.SECRETKEY.name()).notEmpty();
        check(AES_TRANSFORMATION, "AES mode")
                .notBlank()
                .constraint(aesMode -> aesMode.contains("/GCM/"), "must be in GCM mode: ECB/CBC are vulnerable");

        // first bytes are salt
        final byte[] salt = Arrays.copyOfRange(encrypted, 0, AES_SALT_LENGTH_BYTES);
        // next bytes are IV
        final byte[] iv = Arrays.copyOfRange(encrypted, AES_SALT_LENGTH_BYTES,
                AES_SALT_LENGTH_BYTES + AES_IV_LENGTH_BYTES);
        // the rest is the actual ciphertext
        final byte[] ciphertext =
                Arrays.copyOfRange(encrypted, AES_SALT_LENGTH_BYTES + AES_IV_LENGTH_BYTES, encrypted.length);

        // derive the same key using the salt and secret key
        SecretKey aesKey = deriveAesKey(secretKey, salt);
        // authentication tag preventing producing garbage output from garbage input
        var gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

        var cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
        return cipher.doFinal(ciphertext);
    }

    /**
     * Derives an AES key from a (possibly too short or low-entropy) user-provided key and salt using PBKDF2.
     *
     * @param secretKey user-provided secret key
     * @param salt      salt used for key derivation
     * @return AES key
     */
    private SecretKey deriveAesKey(String secretKey, byte[] salt) throws GeneralSecurityException {
        KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH_BITS);
        var factory = SecretKeyFactory.getInstance(SECRET_KEY_FACTORY_ALGORITHM);
        byte[] key = factory.generateSecret(spec).getEncoded();

        return new SecretKeySpec(key, AES_ALGORITHM);
    }

}
