package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.encoder.BaseNSequenceEncoder;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for {@link BaseNSequenceEncoder} testing.
 */
@RequiredArgsConstructor
public class BaseNSequenceEncoderSteps {

    // DI
    private final TextWorld textWorld;

    private BaseNSequenceEncoder encoder;
    private String encodedValue;
    private Long decodedValue;

    @Given("a BaseNSequenceEncoder with alphabet {string}")
    public void aBaseNSequenceEncoderWithAlphabet(String alphabet) {
        textWorld.setLastException(catchThrowable(() -> encoder = new BaseNSequenceEncoder(alphabet)));
    }

    @When("a BaseNSequenceEncoder with alphabet {string} is constructed")
    public void aBaseNSequenceEncoderWithAlphabetIsConstructed(String alphabet) {
        aBaseNSequenceEncoderWithAlphabet(alphabet);
    }

    /**
     * Encodes the given sequence, remembering the result (or the thrown exception) for the following assertions.
     *
     * @param sequence sequence to encode
     */
    @When("sequence {long} is encoded")
    public void sequenceIsEncoded(long sequence) {
        textWorld.setLastException(catchThrowable(() -> encodedValue = encoder.encode(sequence)));
    }

    @When("a null sequence is encoded")
    public void aNullSequenceIsEncoded() {
        textWorld.setLastException(catchThrowable(() -> encodedValue = encoder.encode(null)));
    }

    @When("a blank value is decoded")
    public void aBlankValueIsDecoded() {
        textWorld.setLastException(catchThrowable(() -> decodedValue = encoder.decode(" ")));
    }

    @When("a value of 64 characters is decoded")
    public void aValueOf64CharactersIsDecoded() {
        // 64 ones exceed the max encoded length of 63 (Long.MAX_VALUE in binary)
        textWorld.setLastException(catchThrowable(() -> decodedValue = encoder.decode("1".repeat(64))));
    }

    @When("an invalid value {string} is decoded")
    public void anInvalidValueIsDecoded(String value) {
        textWorld.setLastException(catchThrowable(() -> decodedValue = encoder.decode(value)));
    }

    @Then("the encoded value should be {string}")
    public void theEncodedValueShouldBe(String expected) {
        assertThat(textWorld.getLastException()).as("lastException").isNull();
        assertThat(encodedValue).as("encoded value").isEqualTo(expected);
    }

    @Then("the encoded value length should be {int}")
    public void theEncodedValueLengthShouldBe(int expectedLength) {
        assertThat(textWorld.getLastException()).as("lastException").isNull();
        assertThat(encodedValue).as("encoded value length").hasSize(expectedLength);
    }

    @Then("decoding {string} should return {long}")
    public void decodingShouldReturn(String value, long expected) {
        assertThat(textWorld.getLastException()).as("lastException").isNull();

        textWorld.setLastException(catchThrowable(() -> decodedValue = encoder.decode(value)));

        assertThat(textWorld.getLastException()).as("lastException").isNull();
        assertThat(decodedValue).as("decoded value").isEqualTo(expected);
    }

    @Then("an IllegalArgumentException naming {string} should be thrown")
    public void anIllegalArgumentExceptionNamingShouldBeThrown(String name) {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(textWorld.getLastException().getMessage())
                .as("lastException message")
                .contains(name);
    }

    @Then("an IllegalArgumentException should be thrown")
    public void anIllegalArgumentExceptionShouldBeThrown() {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isInstanceOf(IllegalArgumentException.class);
    }
}
