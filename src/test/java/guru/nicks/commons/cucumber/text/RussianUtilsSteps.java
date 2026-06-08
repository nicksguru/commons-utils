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
import java.util.List;

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

        Field lookupField = RussianUtils.class.getDeclaredField("lookupForMeanings");
        lookupField.setAccessible(true);
        lookupField.set(null, null);

        Field getLemmaField = RussianUtils.class.getDeclaredField("getLemma");
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
     * Requests the lookupForMeanings method handle from RussianUtils.
     */
    @When("lookupForMeanings method handle is requested")
    public void lookupForMeaningsMethodHandleIsRequested() {
        MethodHandle[] handle = new MethodHandle[1];
        Throwable thrown = catchThrowable(() ->
                handle[0] = RussianUtils.getLookupForMeaningsMethod());
        textWorld.setLastException(thrown);

        if (thrown == null) {
            textWorld.setOutput(handle[0] != null ? "not null" : "null");
        }
    }

    /**
     * Requests the getLemma method handle from RussianUtils.
     */
    @When("getLemma method handle is requested")
    public void getLemmaMethodHandleIsRequested() {
        MethodHandle[] handle = new MethodHandle[1];
        Throwable thrown = catchThrowable(() ->
                handle[0] = RussianUtils.getGetLemmaMethod());
        textWorld.setLastException(thrown);

        if (thrown == null) {
            textWorld.setOutput((handle[0] != null)
                    ? "not null"
                    : "null");
        }
    }

    /**
     * Requests the lookupForMeanings method handle twice to verify caching behavior.
     */
    @When("lookupForMeanings method handle is requested twice")
    public void lookupForMeaningsMethodHandleIsRequestedTwice() {
        MethodHandle[] first = new MethodHandle[1];
        MethodHandle[] second = new MethodHandle[1];

        Throwable thrown = catchThrowable(() -> {
            first[0] = RussianUtils.getLookupForMeaningsMethod();
            second[0] = RussianUtils.getLookupForMeaningsMethod();
        });
        textWorld.setLastException(thrown);

        if (thrown == null) {
            firstMethodHandle = first[0];
            secondMethodHandle = second[0];
        }
    }

    /**
     * Requests the getLemma method handle twice to verify caching behavior.
     */
    @When("getLemma method handle is requested twice")
    public void getLemmaMethodHandleIsRequestedTwice() {
        MethodHandle[] first = new MethodHandle[1];
        MethodHandle[] second = new MethodHandle[1];

        Throwable thrown = catchThrowable(() -> {
            first[0] = RussianUtils.getGetLemmaMethod();
            second[0] = RussianUtils.getGetLemmaMethod();
        });
        textWorld.setLastException(thrown);

        if (thrown == null) {
            firstMethodHandle = first[0];
            secondMethodHandle = second[0];
        }
    }

    /**
     * Lemmatizes a Russian word using the AOT library method handles.
     */
    @When("Russian word {string} is lemmatized")
    public void russianWordIsLemmatized(String word) {
        Throwable thrown = catchThrowable(() -> {
            MethodHandle lookupMethod = RussianUtils.getLookupForMeaningsMethod();
            MethodHandle getLemmaMethod = RussianUtils.getGetLemmaMethod();

            // Call lookupForMeanings to get meanings for the word
            @SuppressWarnings("unchecked")
            List<?> meanings = (List<?>) lookupMethod.invoke(word);

            if (meanings != null && !meanings.isEmpty()) {
                // Get the first meaning and extract its lemma
                Object firstMeaning = meanings.getFirst();
                Object lemma = getLemmaMethod.invoke(firstMeaning);
                textWorld.setOutput(lemma != null ? lemma.toString() : "");
            } else {
                textWorld.setOutput("");
            }
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
