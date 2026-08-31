package guru.nicks.commons.cucumber.text;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.text.NgramUtils;
import guru.nicks.commons.utils.text.NgramUtilsConfig;
import guru.nicks.commons.utils.text.TextUtils;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class NgramUtilsSteps {

    // DI
    private final TextWorld textWorld;

    // mode used by the latest 'ngrams are created from...' step, to compare against the String-based overload
    private NgramUtils.Mode lastMode;

    @When("prefix ngrams are created")
    public void prefixNgramsAreCreated() {
        textWorld.setOutput(new ArrayList<>(
                NgramUtils.createNgrams(textWorld.getInput(), NgramUtils.Mode.PREFIX, NgramSettings.INSTANCE)));
    }

    @When("infix ngrams are created")
    public void infixNgramsAreCreated() {
        textWorld.setOutput(new ArrayList<>(
                NgramUtils.createNgrams(textWorld.getInput(), NgramUtils.Mode.INFIX, NgramSettings.INSTANCE)));
    }

    @When("prefix and infix ngrams are created")
    public void prefixAndInfixNgramsAreCreated() {
        textWorld.setOutput(new ArrayList<>(
                NgramUtils.createNgrams(textWorld.getInput(), NgramUtils.Mode.ALL, NgramSettings.INSTANCE)));
    }

    @When("ngrams are created from unique words in {word} mode")
    public void ngramsAreCreatedFromUniqueWords(String modeName) {
        lastMode = NgramUtils.Mode.valueOf(modeName);

        // same tokenization the String-based version performs internally
        var uniqueWords = TextUtils.collectUniqueWords(textWorld.getInput(), NgramSettings.INSTANCE.isReduceAccents());
        textWorld.setOutput(new ArrayList<>(NgramUtils.createNgrams(uniqueWords, lastMode, NgramSettings.INSTANCE)));
    }

    @When("ngrams are created from an empty words collection in {word} mode")
    public void ngramsAreCreatedFromAnEmptyWordsCollection(String modeName) {
        lastMode = NgramUtils.Mode.valueOf(modeName);

        var emptyWords = new TreeSet<String>();
        textWorld.setOutput(new ArrayList<>(NgramUtils.createNgrams(emptyWords, lastMode, NgramSettings.INSTANCE)));
    }

    @Then("output should equal ngrams created from the input string")
    public void outputShouldEqualNgramsCreatedFromTheInputString() {
        var expected = new ArrayList<>(
                NgramUtils.createNgrams(textWorld.getInput(), lastMode, NgramSettings.INSTANCE));

        assertThat(textWorld.getOutput())
                .as("ngrams created from pre-tokenized words")
                .isEqualTo(expected);
    }

    /**
     * Enabled Russian morphological analysis - to be passed to
     * {@link NgramUtils#createNgrams(String, NgramUtils.Mode, NgramUtilsConfig)}.
     */
    public interface NgramSettings extends NgramUtilsConfig {

        NgramSettings INSTANCE = new NgramSettings() {
        };

        @Override
        default boolean tryRussianMorphAnalysis() {
            return true;
        }

    }

}
