package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.utils.NgramUtils;
import guru.nicks.utils.NgramUtilsConfig;

import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;

@RequiredArgsConstructor
public class NgramUtilsSteps {

    // DI
    private final TextWorld textWorld;

    @When("prefix ngrams are created")
    public void prefixNgramsAreCreated() {
        textWorld.setOutput(new ArrayList<>(
                NgramUtils.createNgrams(textWorld.getInput(), NgramUtils.Mode.PREFIX_ONLY, NgramUtilsConfig.DEFAULT)));
    }

    @When("infix ngrams are created")
    public void infixNgramsAreCreated() {
        textWorld.setOutput(new ArrayList<>(
                NgramUtils.createNgrams(textWorld.getInput(), NgramUtils.Mode.INFIX_ONLY, NgramUtilsConfig.DEFAULT)));
    }

    @When("prefix and infix ngrams are created")
    public void prefixAndInfixNgramsAreCreated() {
        textWorld.setOutput(new ArrayList<>(
                NgramUtils.createNgrams(textWorld.getInput(), NgramUtils.Mode.ALL, NgramUtilsConfig.DEFAULT)));
    }

}
