package guru.nicks.commons.cucumber.text;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.text.TextUtils;

import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;

@RequiredArgsConstructor
public class TextUtilsSteps {

    // DI
    private final TextWorld textWorld;

    @When("comma-separated string is parsed")
    public void commaSeparatedStringIsParsed() {
        textWorld.setOutput(new ArrayList<>(
                TextUtils.collectUniqueCommaSeparated(textWorld.getInput())));
    }

    @When("unique words are collected, reducing accented characters")
    public void uniqueWordsAreCollected() {
        textWorld.setOutput(new ArrayList<>(
                TextUtils.collectUniqueWords(textWorld.getInput(), true)));
    }

    @When("string is split into words")
    public void stringIsSplitIntoWords() {
        textWorld.setOutput(
                TextUtils.splitIntoWords(textWorld.getInput()));
    }

    @When("accented characters are reduced")
    public void accentedCharactersAreReduced() {
        textWorld.setOutput(
                TextUtils.reduceAccents(textWorld.getInput()));
    }

    @When("punctuation is removed")
    public void punctuationIsRemoved() {
        textWorld.setOutput(
                TextUtils.removePunctuation(textWorld.getInput()));
    }

    @When("magnitude of count is detected")
    public void magnitudeOfCountIsDetected() {
        textWorld.setOutput(TextUtils.getMagnitudeOfCount(
                Long.parseLong(textWorld.getInput())));

    }

}
