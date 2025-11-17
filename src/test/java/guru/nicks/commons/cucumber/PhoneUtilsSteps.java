package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.PhoneNumberUtils;

import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PhoneUtilsSteps {

    // DI
    private final TextWorld textWorld;

    @When("phone number is validated and normalized")
    public void phoneNumberIsValidatedAndNormalized() {
        textWorld.setOutput(
                PhoneNumberUtils.normalizeInternationalPhoneNumber(textWorld.getInput()));
    }

}
