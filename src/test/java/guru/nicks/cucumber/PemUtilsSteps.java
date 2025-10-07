package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.utils.PemUtils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.security.PrivateKey;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for testing PEM utilities.
 */
@RequiredArgsConstructor
public class PemUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private String pemString;
    private String fixedPem;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    @Given("a PEM string {string}")
    public void aPemString(String pem) {
        pemString = pem;
    }

    @Given("a blank PEM string")
    public void aBlankPemString() {
        pemString = "   ";
    }

    @Given("a valid key pair PEM")
    public void aValidKeyPairPem() {
        // using a minimal valid RSA key pair for testing
        pemString = """
                -----BEGIN RSA PRIVATE KEY-----
                MIIBOgIBAAJBAKj34GkxFhD90vcNLYLInFEX6Ppy1tPf9Cnzj4p4WGeKLs1Pt8Qu
                KUpRKfFLfRYC9AIKjbJTWit+CqvjWYzvQwECAwEAAQJAIJLixBy2qpFoS4DSmoEm
                o3qGy0t6z09AIJtH+5OeRV1be+N4cDYJKffGzDa88vQENZiRm0GRq6a+HPGQMd2k
                TQIhAKMSvzIBnni7ot/OSie2TmJLY4SwTQAevXysE2RbFDYdAiEBCUEaRQnMnbp7
                9mxDXDf6AU0cN/RPBjb9qSHDcWZHGzUCIG2Es59z8ugGrDY+pxLQnwfotadxd+Uy
                v/Ow5T0q5gIJAiEAyS4RaI9YG8EWx/2w0T67ZUVAw8eOMB6BIUg0Xcu+3okCIBOs
                /5OiPgoTdSy7bcF9IGpSE8ZgGKzgYQVZeN97YE00
                -----END RSA PRIVATE KEY-----
                """;
    }

    @Given("a valid public key PEM")
    public void aValidPublicKeyPem() {
        // using a minimal valid RSA public key for testing
        pemString = """
                -----BEGIN PUBLIC KEY-----
                MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKj34GkxFhD90vcNLYLInFEX6Ppy1tPf
                9Cnzj4p4WGeKLs1Pt8QuKUpRKfFLfRYC9AIKjbJTWit+CqvjWYzvQwECAwEAAQ==
                -----END PUBLIC KEY-----
                """;
    }

    @When("the PEM is fixed")
    public void thePemIsFixed() {
        Throwable thrown = catchThrowable(() ->
                fixedPem = PemUtils.fixPem(pemString));
        textWorld.setLastException(thrown);
    }

    @When("the private key is retrieved from the key pair")
    public void thePrivateKeyIsRetrievedFromTheKeyPair() {
        Throwable thrown = catchThrowable(() ->
                privateKey = PemUtils.retrievePrivateKeyFromKeyPair(pemString));

        textWorld.setLastException(thrown);
    }

    @When("the public key is retrieved from the key pair")
    public void thePublicKeyIsRetrievedFromTheKeyPair() {
        Throwable thrown = catchThrowable(() ->
                publicKey = PemUtils.retrievePublicKeyFromKeyPair(pemString));

        textWorld.setLastException(thrown);
    }

    @When("the public key is parsed")
    public void thePublicKeyIsParsed() {
        Throwable thrown = catchThrowable(() ->
                publicKey = PemUtils.parsePublicKey(pemString));

        textWorld.setLastException(thrown);
    }

    @Then("the fixed PEM should be {string}")
    public void theFixedPemShouldBe(String expected) {
        assertThat(fixedPem)
                .as("fixedPem")
                .isEqualTo(expected);
    }

    @Then("a private key should be returned")
    public void aPrivateKeyShouldBeReturned() {
        assertThat(privateKey)
                .as("privateKey")
                .isNotNull();

        assertThat(privateKey.getAlgorithm())
                .as("privateKey.algorithm")
                .isEqualTo("RSA");
    }

    @Then("a public key should be returned")
    public void aPublicKeyShouldBeReturned() {
        assertThat(publicKey)
                .as("publicKey")
                .isNotNull();

        assertThat(publicKey.getAlgorithm())
                .as("publicKey.algorithm")
                .isEqualTo("RSA");
    }
}
