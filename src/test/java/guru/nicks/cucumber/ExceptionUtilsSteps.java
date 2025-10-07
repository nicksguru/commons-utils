package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.utils.ExceptionUtils;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for {@link ExceptionUtils} testing.
 */
@RequiredArgsConstructor
public class ExceptionUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private Throwable testException;

    @Given("an exception of type {string} with message {string} is created")
    public void anExceptionOfTypeWithMessageIsCreated(String exceptionType, String message) {
        var throwable = catchThrowable(() -> {
            try {
                Class<?> exceptionClass = Class.forName("java.lang." + exceptionType);
                Constructor<?> constructor = exceptionClass.getConstructor(String.class);
                testException = (Throwable) constructor.newInstance(
                        StringUtils.isNotBlank(message)
                                ? message
                                : null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create exception", e);
            }
        });

        textWorld.setLastException(throwable);
    }

    @Given("exception is null")
    public void exceptionIsNull() {
        testException = null;
    }

    @Given("exception has root cause of type {string} with message {string}")
    public void exceptionHasRootCauseOfTypeWithMessage(String rootCauseType, String rootCauseMessage) {
        var throwable = catchThrowable(() -> {
            try {
                Class<?> rootCauseClass = Class.forName("java.lang." + rootCauseType);
                Constructor<?> rootCauseConstructor = rootCauseClass.getConstructor(String.class);

                var rootCause = (Throwable) rootCauseConstructor.newInstance(
                        StringUtils.isNotBlank(rootCauseMessage)
                                ? rootCauseMessage
                                : null);

                // create a new exception with the root cause
                Class<?> exceptionClass = testException.getClass();
                Constructor<?> constructor = exceptionClass.getConstructor(String.class, Throwable.class);
                testException = (Throwable) constructor.newInstance(testException.getMessage(), rootCause);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create exception with root cause", e);
            }
        });

        textWorld.setLastException(throwable);
    }

    @Given("an exception with stack trace containing {string} is created")
    public void anExceptionWithStackTraceContainingIsCreated(String className) {
        var throwable = catchThrowable(() -> {
            var exception = new RuntimeException("Test exception");

            // Create a mock stack trace element with the specified class name
            var stackTraceElement = new StackTraceElement(
                    className,
                    "testMethod",
                    "TestFile.java",
                    42);

            // Add some real stack trace elements and the mock one
            var currentStackTrace = exception.getStackTrace();
            var newStackTrace = new StackTraceElement[currentStackTrace.length + 1];
            newStackTrace[0] = stackTraceElement;
            System.arraycopy(currentStackTrace, 0, newStackTrace, 1, currentStackTrace.length);

            exception.setStackTrace(newStackTrace);
            testException = exception;
        });

        textWorld.setLastException(throwable);
    }

    @When("exception is formatted with compact stack trace")
    public void exceptionIsFormattedWithCompactStackTrace() {
        var throwable = catchThrowable(() -> {
            String result = ExceptionUtils.formatWithCompactStackTrace(testException);
            textWorld.setOutput(result);
        });

        textWorld.setLastException(throwable);
    }

    @Then("output should contain exception class name {string}")
    public void outputShouldContainExceptionClassName(String exceptionType) {
        assertThat(textWorld.getOutput())
                .as("output")
                .isNotEmpty();

        assertThat(textWorld.getOutput().getFirst())
                .as("formatted exception output")
                .contains("java.lang." + exceptionType);
    }

    @And("output should contain message {string}")
    public void outputShouldContainMessage(String expectedMessage) {
        assertThat(textWorld.getOutput())
                .as("output")
                .isNotEmpty();

        assertThat(textWorld.getOutput().getFirst())
                .as("formatted exception output")
                .contains(expectedMessage);
    }

    @And("output should contain {string}")
    public void outputShouldContain(String expectedText) {
        assertThat(textWorld.getOutput())
                .as("output")
                .isNotEmpty();

        assertThat(textWorld.getOutput().getFirst())
                .as("formatted exception output")
                .contains(expectedText);
    }

    @And("output should contain root cause {string}")
    public void outputShouldContainRootCause(String expectedRootCause) {
        assertThat(textWorld.getOutput())
                .as("output")
                .isNotEmpty();

        assertThat(textWorld.getOutput().getFirst())
                .as("formatted exception output")
                .contains("with root cause: java.lang." + expectedRootCause);
    }

    @And("trivial frames should be omitted from stack trace")
    public void trivialFramesShouldBeOmittedFromStackTrace() {
        assertThat(textWorld.getOutput())
                .as("output")
                .isNotEmpty();

        String output = textWorld.getOutput().getFirst();

        // Verify that some common trivial frames are not present
        ExceptionUtils.OMITTED_CLASS_PREFIXES.forEach(prefix ->
                assertThat(output)
                        .as("formatted exception output should not contain trivial frame: " + prefix)
                        .doesNotContain(prefix));
    }

    @Then("stack trace {string} contain {string}")
    public void stackTraceShouldContain(String shouldContain, String className) {
        assertThat(textWorld.getOutput())
                .as("output")
                .isNotEmpty();

        String output = textWorld.getOutput().getFirst();

        switch (shouldContain) {
            case "should" -> assertThat(output)
                    .as("formatted exception output should contain: " + className)
                    .contains(className);

            case "should not" -> assertThat(output)
                    .as("formatted exception output should not contain: " + className)
                    .doesNotContain(className);

            default -> throw new IllegalArgumentException("Invalid should contain value: '" + shouldContain + "'");
        }
    }

}
