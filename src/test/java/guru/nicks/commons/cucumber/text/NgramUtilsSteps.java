package guru.nicks.commons.cucumber.text;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.text.NgramUtils;
import guru.nicks.commons.utils.text.NgramUtilsConfig;

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
                NgramUtils.createNgrams(textWorld.getInput(), NgramUtils.Mode.PREFIX_ONLY, NgramSettings.INSTANCE)));
    }

    @When("infix ngrams are created")
    public void infixNgramsAreCreated() {
        textWorld.setOutput(new ArrayList<>(
                NgramUtils.createNgrams(textWorld.getInput(), NgramUtils.Mode.INFIX_ONLY, NgramSettings.INSTANCE)));
    }

    @When("prefix and infix ngrams are created")
    public void prefixAndInfixNgramsAreCreated() {
        textWorld.setOutput(new ArrayList<>(
                NgramUtils.createNgrams(textWorld.getInput(), NgramUtils.Mode.ALL, NgramSettings.INSTANCE)));
    }

    /**
     * Enabled Russian morphological analysis - to be passed to
     * {@link NgramUtils#createNgrams(String, NgramUtils.Mode, NgramUtilsConfig)}.
     */
    public interface NgramSettings extends NgramUtilsConfig {

        NgramSettings INSTANCE = new NgramSettings() {
        };

        @Override
        default boolean tryMorphAnalysis() {
            return true;
        }

    }

}
