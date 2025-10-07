package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.utils.TimeUtils;

import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public class TimeUtilsSteps {

    // DI
    private final TextWorld textWorld;

    @When("H:M:S duration is converted to seconds")
    public void hms_duration_is_converted_to_seconds() {
        textWorld.setOutput(Objects.toString(
                TimeUtils.convertHmsDurationToSeconds(textWorld.getInput()), ""));
    }

}
