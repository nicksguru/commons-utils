package guru.nicks.commons.cucumber.text;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.text.RussianUtils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Cucumber step definitions for testing {@link RussianUtils}.
 */
@RequiredArgsConstructor
public class RussianUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private MethodHandle firstMethodHandle;
    private MethodHandle secondMethodHandle;

    /**
     * Resets the initialization state of RussianUtils to allow testing different scenarios. This uses reflection to set
     * the {@code initializedOrFailed} field to false.
     */
    @SneakyThrows
    private void resetRussianUtilsState() {
        Field initializedField = RussianUtils.class.getDeclaredField("initializedOrFailed");
        initializedField.setAccessible(true);
        initializedField.setBoolean(null, false);

        Field lookupField = RussianUtils.class.getDeclaredField("lookupForMeaningsMethod");
        lookupField.setAccessible(true);
        lookupField.set(null, null);

        Field getLemmaField = RussianUtils.class.getDeclaredField("getLemmaMethod");
        getLemmaField.setAccessible(true);
        getLemmaField.set(null, null);
    }

    /**
     * Simulates the scenario where the AOT library is available on the classpath. In reality, this test assumes the
     * library is present (as it should be in the test environment).
     */
    @Given("Russian AOT library is available")
    public void russianAotLibraryIsAvailable() {
        resetRussianUtilsState();
    }

    /**
     * Lemmatizes a Russian word using the AOT library method handles.
     */
    @When("Russian word {string} is lemmatized")
    public void russianWordIsLemmatized(String word) {
        Throwable thrown = catchThrowable(() -> {
            String lemma = RussianUtils.getWordLemma(word);
            textWorld.setOutput(lemma);
        });
        textWorld.setLastException(thrown);
    }

    /**
     * Verifies that the method handle is not null.
     */
    @Then("method handle should not be null")
    public void methodHandleShouldNotBeNull() {
        assertThat(textWorld.getOutput())
                .as("method handle output")
                .isNotNull()
                .containsExactly("not null");
    }

    /**
     * Verifies that both method handles are the same instance (caching behavior).
     */
    @Then("both handles should be the same")
    public void bothHandlesShouldBeTheSame() {
        assertThat(firstMethodHandle)
                .as("first method handle")
                .isNotNull();

        assertThat(secondMethodHandle)
                .as("second method handle")
                .isNotNull();

        assertThat(firstMethodHandle)
                .as("method handles")
                .isSameAs(secondMethodHandle);
    }

}
