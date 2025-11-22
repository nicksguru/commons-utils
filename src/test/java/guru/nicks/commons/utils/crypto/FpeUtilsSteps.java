package guru.nicks.commons.utils.crypto;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.crypto.FpeUtils.SequenceEncryptor;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class FpeUtilsSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private Supplier<Long> nextValueSupplier;
    private AutoCloseable closeableMocks;

    private SequenceEncryptor sequenceEncryptor;
    private EncryptorCreationRequest creationRequest;

    private String encryptedValue;
    private long decryptedValue;

    @DataTableType
    public EncryptorCreationRequest createEncryptorCreationRequest(Map<String, String> row) {
        return EncryptorCreationRequest.builder()
                .zeroPadValueDigits(Integer.parseInt(
                        row.get("zeroPadValueDigits")))
                .key(row.get("key"))
                .tweak(row.get("tweak"))
                .supplierIsNull(Boolean.parseBoolean(
                        row.get("supplierIsNull")))
                .build();
    }

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("an FF1 sequence encryptor is created with the following arguments")
    public void anFf1SequenceEncryptorIsCreatedWithTheFollowingArguments(EncryptorCreationRequest creationRequest) {
        this.creationRequest = creationRequest;
    }

    @When("the encryptor is instantiated")
    public void theEncryptorIsInstantiated() {
        var throwable = catchThrowable(() ->
                FpeUtils.createFf1SequenceEncryptor(
                        creationRequest.isSupplierIsNull() ? null : nextValueSupplier,
                        creationRequest.getZeroPadValueDigits(),
                        StringUtils.isNotBlank(creationRequest.getKey()) ? creationRequest.getKey() : null,
                        StringUtils.isNotBlank(creationRequest.getTweak()) ? creationRequest.getTweak() : null
                )
        );
        textWorld.setLastException(throwable);
    }

    @Given("an FF1 sequence encryptor is created with key {string}, tweak {string}, and padding of {int} digits")
    public void anFf1SequenceEncryptorIsCreatedWithKeyTweakAndPaddingOfDigits(String key, String tweak, int padding) {
        sequenceEncryptor = FpeUtils.createFf1SequenceEncryptor(nextValueSupplier, padding, key, tweak);
    }

    @Given("the sequence value supplier will return {long}")
    public void theSequenceValueSupplierWillReturn(long sequenceValue) {
        when(nextValueSupplier.get())
                .thenReturn(sequenceValue);
    }

    @When("the next encrypted value is requested")
    public void theNextEncryptedValueIsRequested() {
        var throwable = catchThrowable(() ->
                encryptedValue = sequenceEncryptor.getNext());
        textWorld.setLastException(throwable);
    }

    @Then("the encrypted value is not blank")
    public void theEncryptedValueIsNotBlank() {
        assertThat(encryptedValue)
                .as("encryptedValue")
                .isNotBlank();
    }

    @Then("decrypting the value returns {long}")
    public void decryptingTheValueReturns(long expectedValue) {
        var throwable = catchThrowable(() -> decryptedValue = sequenceEncryptor.decrypt(encryptedValue));
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

    @Value
    @Builder
    public static class EncryptorCreationRequest {

        Integer zeroPadValueDigits;
        String key;
        String tweak;
        boolean supplierIsNull;

    }
}
