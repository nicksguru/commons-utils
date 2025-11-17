package guru.nicks.commons.cucumber;

import guru.nicks.commons.validation.InternationalPhoneNumberFormatValidator;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.validation.ConstraintValidatorContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing the InternationalPhoneNumberFormat validation annotation.
 */
public class InternationalPhoneNumberSteps {

    @Mock
    private ConstraintValidatorContext validatorContext;
    private AutoCloseable closeableMocks;

    private InternationalPhoneNumberFormatValidator validator;
    private String phoneNumber;
    private boolean validationResult;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        validator = new InternationalPhoneNumberFormatValidator();
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("a phone number {string}")
    public void aPhoneNumber(String phoneNumber) {
        this.phoneNumber = switch (phoneNumber) {
            case "<null>" -> null;
            case "<empty>" -> "";
            case "<whitespaces>" -> " ";
            default -> phoneNumber;
        };
    }

    @When("the phone number is validated")
    public void thePhoneNumberIsValidated() {
        validationResult = validator.isValid(phoneNumber, validatorContext);
    }

    @Then("the validation result should be {booleanValue}")
    public void theValidationResultShouldBe(boolean expected) {
        assertThat(validationResult)
                .as("Validation result")
                .isEqualTo(expected);
    }

}
