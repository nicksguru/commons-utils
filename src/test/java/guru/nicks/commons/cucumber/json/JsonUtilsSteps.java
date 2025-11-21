package guru.nicks.commons.cucumber.json;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.json.JsonUtils;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class JsonUtilsSteps {

    // DI
    private final TextWorld textWorld;

    @When("sensitive JSON fields are masked")
    public void sensitiveJsonFieldsAreMasked() {
        textWorld.setOutput(
                JsonUtils.maskSensitiveJsonFields(
                        // passing bytes internally calls the method accepting a string,
                        // which increases test coverage
                        textWorld.getInput().getBytes(StandardCharsets.UTF_8)));
    }

    @Then("masked JSON should contain {string} for {string}")
    public void maskedJsonShouldContain(String maskedValue, String fieldName) {
        assertThat(textWorld.getOutput().getFirst())
                .contains("\"" + fieldName + "\":" + maskedValue);
    }

    @Then("masked JSON is empty")
    public void maskedJsonIsEmpty() {
        assertThat(textWorld.getOutput()).isEmpty();
    }

}
