package guru.nicks.commons.cucumber.crypto;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.crypto.DammChecksumUtils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for testing {@link DammChecksumUtils} functionality.
 */
@RequiredArgsConstructor
public class DammChecksumUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private String payload;
    private String value;

    private char checksum;
    private char checksum2;
    private boolean isValid;

    private DammChecksumUtils.Impl implementation;

    @Given("a payload {string}")
    public void aPayload(String payload) {
        this.payload = payload;
    }

    @Given("a value with checksum {string}")
    public void aValueWithChecksum(String value) {
        this.value = value;
    }

    @Given("a value with invalid checksum {string}")
    public void aValueWithInvalidChecksum(String value) {
        this.value = value;
    }

    @Given("a valid decimal checksummed value {string}")
    public void aValidDecimalChecksummedValue(String original) {
        value = original;
    }

    @Given("a valid Crockford Base32 checksummed value {string}")
    public void aValidCrockfordBase32ChecksummedValue(String original) {
        value = original;
    }

    @Given("a valid alphanumeric checksummed value {string}")
    public void aValidAlphanumericChecksummedValue(String original) {
        value = original;
    }

    @Given("an empty string")
    public void anEmptyString() {
        value = "";
    }

    @Given("a single character {string}")
    public void aSingleCharacter(String character) {
        value = character;
    }

    @Given("a payload with invalid character {string}")
    public void aPayloadWithInvalidCharacter(String payload) {
        this.payload = payload;
    }

    @Given("a value with invalid character {string}")
    public void aValueWithInvalidCharacter(String value) {
        this.value = value;
    }

    @Given("an empty payload")
    public void anEmptyPayload() {
        payload = "";
    }

    @When("Damm checksum is computed using DECIMAL implementation")
    public void dammChecksumIsComputedUsingDECIMALImplementation() {
        implementation = DammChecksumUtils.DECIMAL;

        var throwable = catchThrowable(() ->
                checksum = implementation.compute(payload));
        textWorld.setLastException(throwable);
    }

    @When("Damm checksum is computed using CROCKFORD_BASE32 implementation")
    public void dammChecksumIsComputedUsingCROCKFORD_BASE32Implementation() {
        implementation = DammChecksumUtils.CROCKFORD_BASE32;

        var throwable = catchThrowable(() ->
                checksum = implementation.compute(payload));
        textWorld.setLastException(throwable);
    }

    @When("Damm checksum is computed using ALPHANUMERIC implementation")
    public void dammChecksumIsComputedUsingALPHANUMERICImplementation() {
        implementation = DammChecksumUtils.ALPHANUMERIC;

        var throwable = catchThrowable(() ->
                checksum = implementation.compute(payload));
        textWorld.setLastException(throwable);
    }

    @When("the value is validated using DECIMAL implementation")
    public void theValueIsValidatedUsingDECIMALImplementation() {
        implementation = DammChecksumUtils.DECIMAL;
        isValid = implementation.isValid(value);
    }

    @When("the value is validated using CROCKFORD_BASE32 implementation")
    public void theValueIsValidatedUsingCROCKFORD_BASE32Implementation() {
        implementation = DammChecksumUtils.CROCKFORD_BASE32;
        isValid = implementation.isValid(value);
    }

    @When("the value is validated using ALPHANUMERIC implementation")
    public void theValueIsValidatedUsingALPHANUMERICImplementation() {
        implementation = DammChecksumUtils.ALPHANUMERIC;
        isValid = implementation.isValid(value);
    }

    @When("a single digit is corrupted to {string}")
    public void aSingleDigitIsCorruptedTo(String corrupted) {
        value = corrupted;
    }

    @When("a single character is corrupted to {string}")
    public void aSingleCharacterIsCorruptedTo(String corrupted) {
        value = corrupted;
    }

    @When("adjacent digits are transposed to {string}")
    public void adjacentDigitsAreTransposedTo(String transposed) {
        value = transposed;
    }

    @When("adjacent characters are transposed to {string}")
    public void adjacentCharactersAreTransposedTo(String transposed) {
        value = transposed;
    }

    @When("Damm checksum is computed twice using DECIMAL implementation")
    public void dammChecksumIsComputedTwiceUsingDECIMALImplementation() {
        implementation = DammChecksumUtils.DECIMAL;
        checksum = implementation.compute(payload);
        checksum2 = implementation.compute(payload);
    }

    @When("Damm checksum is computed twice using CROCKFORD_BASE32 implementation")
    public void dammChecksumIsComputedTwiceUsingCROCKFORD_BASE32Implementation() {
        implementation = DammChecksumUtils.CROCKFORD_BASE32;
        checksum = implementation.compute(payload);
        checksum2 = implementation.compute(payload);
    }

    @When("Damm checksum is computed twice using ALPHANUMERIC implementation")
    public void dammChecksumIsComputedTwiceUsingALPHANUMERICImplementation() {
        implementation = DammChecksumUtils.ALPHANUMERIC;
        checksum = implementation.compute(payload);
        checksum2 = implementation.compute(payload);
    }

    @Then("the checksum character should be {string}")
    public void theChecksumCharacterShouldBe(String expectedChecksum) {
        assertThat(checksum)
                .as("checksum character")
                .isEqualTo(expectedChecksum.charAt(0));
    }

    @Then("the value should be valid")
    public void theValueShouldBeValid() {
        assertThat(isValid)
                .as("value validity")
                .isTrue();
    }

    @Then("the value should be invalid")
    public void theValueShouldBeInvalid() {
        assertThat(isValid)
                .as("value validity")
                .isFalse();
    }

    @Then("both Damm checksums should be identical")
    public void bothDammChecksumsShouldBeIdentical() {
        assertThat(checksum)
                .as("first checksum")
                .isEqualTo(checksum2);
    }

}
