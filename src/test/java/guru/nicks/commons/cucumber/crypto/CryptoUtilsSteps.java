package guru.nicks.commons.cucumber.crypto;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.crypto.CryptoUtils;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing {@link CryptoUtils}.
 */
@RequiredArgsConstructor
public class CryptoUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private byte[] inputBytes;
    private String encodedString;
    private byte[] decodedBytes;
    private String secretKey;
    private String hmacResult;
    private byte[] encryptedBytes;
    private byte[] decryptedBytes;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    @Mock
    private SecretKeySpec mockSecretKeySpec;
    private AutoCloseable closeableMocks;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("a byte array with content {string}")
    public void aByteArrayWithContent(String content) {
        inputBytes = content.getBytes(StandardCharsets.UTF_8);
    }

    @Given("a null byte array")
    public void aNullByteArray() {
        inputBytes = null;
    }

    @Given("a secret key {string}")
    public void aSecretKey(String key) {
        secretKey = key;
    }

    @Given("RSA key pair is available")
    public void rsaKeyPairIsAvailable() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();
    }

    @Given("a null RSA public key")
    public void aNullRsaPublicKey() {
        publicKey = null;
    }

    @When("the byte array is encoded to Base64")
    public void theByteArrayIsEncodedToBase64() {
        encodedString = CryptoUtils.encodeBase64(inputBytes);
    }

    @When("the encoded string is decoded from Base64")
    public void theEncodedStringIsDecodedFromBase64() {
        decodedBytes = CryptoUtils.decodeBase64(encodedString);
    }

    @When("HMAC SHA-512 is calculated and encoded as Base64")
    public void hmacSha512IsCalculatedAndEncodedAsBase64() {
        hmacResult = CryptoUtils.encodeBase64(
                CryptoUtils.calculateHmacSha512(inputBytes, secretKey));
    }

    @When("the byte array is encrypted with RSA public key")
    public void theByteArrayIsEncryptedWithRsaPublicKey() {
        try {
            encryptedBytes = CryptoUtils.rsaEncrypt(inputBytes, publicKey);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("the encrypted result is decrypted with RSA private key")
    public void theEncryptedResultIsDecryptedWithRsaPrivateKey() {
        decryptedBytes = CryptoUtils.rsaDecrypt(encryptedBytes, privateKey);
    }

    @When("the byte array is encrypted with AES")
    public void theByteArrayIsEncryptedWithAes() {
        try {
            encryptedBytes = CryptoUtils.aesEncrypt(inputBytes, secretKey);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("the encrypted result is decrypted with AES")
    public void theEncryptedResultIsDecryptedWithAes() {
        decryptedBytes = CryptoUtils.aesDecrypt(encryptedBytes, secretKey);
    }

    @Then("the encoded result should be {string}")
    public void theEncodedResultShouldBe(String expected) {
        assertThat(encodedString)
                .as("encoded string")
                .isEqualTo(expected);
    }

    @Then("the decoded result should match the original byte array")
    public void theDecodedResultShouldMatchTheOriginalByteArray() {
        assertThat(decodedBytes)
                .as("decoded bytes")
                .isEqualTo(inputBytes);
    }

    @Then("the HMAC result should be {string}")
    public void theHmacResultShouldBe(String expected) {
        assertThat(hmacResult)
                .as("HMAC result")
                .isEqualTo(expected);
    }

    @Then("the encrypted result should not be empty")
    public void theEncryptedResultShouldNotBeEmpty() {
        assertThat(encryptedBytes)
                .as("encrypted bytes")
                .isNotEmpty();
    }

    @Then("the encrypted result should be different from the original")
    public void theEncryptedResultShouldBeDifferentFromTheOriginal() {
        assertThat(encryptedBytes)
                .as("encrypted bytes")
                .isNotEqualTo(inputBytes);
    }

    @Then("the decrypted result should match the original byte array")
    public void theDecryptedResultShouldMatchTheOriginalByteArray() {
        assertThat(decryptedBytes)
                .as("decrypted bytes")
                .isEqualTo(inputBytes);
    }

}
