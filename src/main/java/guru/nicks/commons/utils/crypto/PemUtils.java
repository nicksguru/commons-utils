package guru.nicks.commons.utils.crypto;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.StringReader;
import java.io.StringWriter;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.regex.Pattern;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;

/**
 * PEM-related utility methods.
 */
@UtilityClass
public class PemUtils {

    /**
     * Matches {@code -----BEGIN [RSA|...]? [PRIVATE|PUBLIC KEY]-----} not ending with a linebreak. The point is to fix
     * such definitions by adding a linebreak after such lines, otherwise they remain unparseable.
     */
    private static final Pattern PEM_START_NO_LINEBREAK_PATTERN = Pattern.compile(
            "(?x)           (-----BEGIN \\s+ \\S*?\\s* (?:PRIVATE|PUBLIC) \\s+ KEY-----)([^\\r\\n])");
    /**
     * Matches {@code -----END [RSA|...]? [PRIVATE|PUBLIC KEY]-----} not starting with a linebreak. The point is to fix
     * such definitions by adding a linebreak before such lines, otherwise they remain unparseable.
     */
    private static final Pattern PEM_END_NO_LINEBREAK_PATTERN = Pattern.compile(
            "(?x)([^\\r\\n])(-----END   \\s+ \\S*?\\s* (?:PRIVATE|PUBLIC) \\s+ KEY-----)");

    /**
     * Fixes PEM, with regard to linebreaks, to make it parseable by {@link PEMParser}.
     *
     * @param pem key pair in PEM format
     * @throws IllegalArgumentException PEM is blank
     */
    public static String fixPem(String pem) {
        checkNotBlank(pem, "pem");

        // ensure linebreak after BEGIN declaration and before END declaration, otherwise parser complains of not being
        // able to decode a Base64 string (which is between those markers)
        String fixed = PEM_START_NO_LINEBREAK_PATTERN.matcher(pem).replaceFirst("$1\n$2");
        fixed = PEM_END_NO_LINEBREAK_PATTERN.matcher(fixed).replaceFirst("$1\n$2");

        return fixed;
    }

    /**
     * Retrieves private key from keypair.
     *
     * @param pem key pair in PEM format
     * @return private key
     */
    @SneakyThrows
    public static PrivateKey retrievePrivateKeyFromKeyPair(String pem) {
        try (PEMParser pemParser = createPemParser(pem)) {
            PEMKeyPair keyPair = (PEMKeyPair) pemParser.readObject();
            return new JcaPEMKeyConverter().getPrivateKey(keyPair.getPrivateKeyInfo());
        }
    }

    /**
     * Retrieves public key from key pair.
     *
     * @param pem key pair in PEM format
     * @return public key
     */
    @SneakyThrows
    public static PublicKey retrievePublicKeyFromKeyPair(String pem) {
        try (PEMParser pemParser = createPemParser(pem)) {
            PEMKeyPair keyPair = (PEMKeyPair) pemParser.readObject();
            return new JcaPEMKeyConverter().getPublicKey(keyPair.getPublicKeyInfo());
        }
    }

    /**
     * Parses public key.
     *
     * @param pem public key alone, in PEM format (Base64-encoded)
     * @return public key
     */
    @SneakyThrows
    public static PublicKey parsePublicKey(String pem) {
        try (PEMParser pemParser = createPemParser(pem)) {
            SubjectPublicKeyInfo keyInfo = (SubjectPublicKeyInfo) pemParser.readObject();
            return new JcaPEMKeyConverter().getPublicKey(keyInfo);
        }
    }

    /**
     * Encodes a key to PEM format.
     *
     * @param key the key to encode
     * @return PEM formatted string
     */
    @SneakyThrows
    public static String encodeToPem(Key key) {
        var result = new StringWriter();
        var pemWriter = new JcaPEMWriter(result);

        pemWriter.writeObject(key);
        pemWriter.close();
        return fixPem(result.toString());
    }

    /**
     * Creates parser for PEM-encoded public key or private+public key pair.
     *
     * @param pem PEM
     * @return PEM parser
     */
    private static PEMParser createPemParser(String pem) {
        return new PEMParser(new StringReader(fixPem(pem)));
    }

}
