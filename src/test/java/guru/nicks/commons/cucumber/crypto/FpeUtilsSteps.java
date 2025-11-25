package guru.nicks.commons.cucumber.crypto;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.crypto.FpeUtils;
import guru.nicks.commons.utils.crypto.FpeUtils.SequenceEncryptor;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class FpeUtilsSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private Supplier<String> nextValueSupplier;
    private AutoCloseable closeableMocks;

    private SequenceEncryptor sequenceEncryptor;

    private String encryptedValue;
    private String decryptedValue;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("an FF1 sequence encryptor is created with key {string}, tweak {string}, alphabet {string}, and padding to {int} positions")
    public void anFf1SequenceEncryptorIsCreatedWithKeyTweakAndPaddingOfDigits(String key, String tweak,
            String alphabet, int padding) {
        textWorld.setLastException(catchThrowable(() ->
                sequenceEncryptor = FpeUtils.createFf1SequenceEncryptor(nextValueSupplier,
                        alphabet,
                        padding,
                        // pad with the first character of the alphabet
                        alphabet.charAt(0),
                        key.getBytes(StandardCharsets.UTF_8),
                        tweak.getBytes(StandardCharsets.UTF_8))));
    }

    @Given("the sequence value supplier will return {string}")
    public void theSequenceValueSupplierWillReturn(String sequenceValue) {
        when(nextValueSupplier.get())
                .thenReturn(String.valueOf(sequenceValue));
    }

    @Given("the sequence number supplier will return {string} and then {string}")
    public void theSequenceNumberSupplierWillReturnsAndThen(String value1, String value2) {
        when(nextValueSupplier.get())
                .thenReturn(value1)
                .thenReturn(value2);
    }

    @When("the next encrypted value is requested")
    public void theNextEncryptedValueIsRequested() {
        var throwable = catchThrowable(() ->
                encryptedValue = sequenceEncryptor.getNextEncrypted());
        textWorld.setLastException(throwable);
    }

    @Then("the encrypted value is not blank")
    public void theEncryptedValueIsNotBlank() {
        assertThat(encryptedValue)
                .as("encryptedValue")
                .isNotBlank();
    }

    @Then("decrypting the value returns {string}")
    public void decryptingTheValueReturns(String expectedValue) {
        var throwable = catchThrowable(() ->
                decryptedValue = sequenceEncryptor.decrypt(encryptedValue));
        textWorld.setLastException(throwable);

        assertThat(decryptedValue)
                .as("decryptedValue")
                .isEqualTo(expectedValue);
    }

    @When("the value {string} is decrypted")
    public void theValueIsDecrypted(String encryptedValue) {
        var throwable = catchThrowable(() ->
                decryptedValue = sequenceEncryptor.decrypt(encryptedValue));
        textWorld.setLastException(throwable);
    }

    @And("the encrypted value should be {string}")
    public void theEncryptedValueShouldBe(String value) {
        assertThat(encryptedValue)
                .as("encryptedValue")
                .isEqualTo(value);

    }
}
