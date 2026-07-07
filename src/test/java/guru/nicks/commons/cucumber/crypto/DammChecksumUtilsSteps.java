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

    @Given("a decimal payload {string}")
    public void aDecimalPayload(String payload) {
        this.payload = payload;
    }

    @Given("a Crockford Base32 payload {string}")
    public void aCrockfordBase32Payload(String payload) {
        this.payload = payload;
    }

    @Given("a decimal value with checksum {string}")
    public void aDecimalValueWithChecksum(String value) {
        this.value = value;
    }

    @Given("a Crockford Base32 value with checksum {string}")
    public void aCrockfordBase32ValueWithChecksum(String value) {
        this.value = value;
    }

    @Given("a decimal value with invalid checksum {string}")
    public void aDecimalValueWithInvalidChecksum(String value) {
        this.value = value;
    }

    @Given("a Crockford Base32 value with invalid checksum {string}")
    public void aCrockfordBase32ValueWithInvalidChecksum(String value) {
        this.value = value;
    }

    @Given("a valid decimal checksummed value {string}")
    public void aValidDecimalChecksummedValue(String original) {
        this.value = original;
    }

    @Given("a valid Crockford Base32 checksummed value {string}")
    public void aValidCrockfordBase32ChecksummedValue(String original) {
        this.value = original;
    }

    @Given("an empty string")
    public void anEmptyString() {
        this.value = "";
    }

    @Given("a single character {string}")
    public void aSingleCharacter(String character) {
        this.value = character;
    }

    @Given("a decimal payload with invalid character {string}")
    public void aDecimalPayloadWithInvalidCharacter(String payload) {
        this.payload = payload;
    }

    @Given("a Crockford Base32 payload with invalid character {string}")
    public void aCrockfordBase32PayloadWithInvalidCharacter(String payload) {
        this.payload = payload;
    }

    @Given("a decimal value with invalid character {string}")
    public void aDecimalValueWithInvalidCharacter(String value) {
        this.value = value;
    }

    @Given("a Crockford Base32 value with invalid character {string}")
    public void aCrockfordBase32ValueWithInvalidCharacter(String value) {
        this.value = value;
    }

    @Given("an empty decimal payload")
    public void anEmptyDecimalPayload() {
        this.payload = "";
    }

    @Given("an empty Crockford Base32 payload")
    public void anEmptyCrockfordBase32Payload() {
        this.payload = "";
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

    @Then("both checksums should be identical")
    public void bothChecksumsShouldBeIdentical() {
        assertThat(checksum)
                .as("first checksum")
                .isEqualTo(checksum2);
    }

}
