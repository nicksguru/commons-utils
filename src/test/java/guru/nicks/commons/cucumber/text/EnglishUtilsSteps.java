package guru.nicks.commons.cucumber.text;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.text.EnglishUtils;

import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EnglishUtilsSteps {

    // DI
    private final TextWorld textWorld;

    @When("stop word check is performed")
    public void stopWordCheckIsPerformed() {
        Boolean result = EnglishUtils.stopWord(textWorld.getInput());
        textWorld.setOutput(result.toString());
    }

    @When("word is lemmatized")
    public void wordIsLemmatized() {
        String lemma = EnglishUtils.getWordLemma(textWorld.getInput());
        textWorld.setOutput(lemma);
    }

}
