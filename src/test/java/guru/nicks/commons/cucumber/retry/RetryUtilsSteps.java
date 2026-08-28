package guru.nicks.commons.cucumber.retry;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.retry.RetryConfig;
import guru.nicks.commons.utils.retry.RetryUtils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@RequiredArgsConstructor
public class RetryUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private final AtomicInteger attemptCount = new AtomicInteger();

    private Duration baseDelay;
    private int maxRetryAttempts;
    private String result;

    private int loggerInvocationCount;
    private int lastLoggerRetriesMade;

    @Given("base delay is {int} ms")
    public void setBaseDelay(int milliseconds) {
        baseDelay = Duration.ofMillis(milliseconds);
    }

    @Given("maximum {int} retry attempts")
    public void setMaxRetryAttempts(int attempts) {
        this.maxRetryAttempts = attempts;
    }

    @When("execute operation that succeeds after {int} invocations")
    public void executeOperationWithFailures(int succeedAfterInvocations) {
        var config = createRetryConfig();

        var throwable = catchThrowable(() ->
                result = RetryUtils.getWithRetry(config, (RetryUtils.Context context) -> {
                            if (attemptCount.incrementAndGet() < succeedAfterInvocations) {
                                throw new RuntimeException("Planned failure");
                            }

                            return "Success";
                        }, this::recordLoggerInvocation
                ));

        textWorld.setLastException(throwable);
    }

    @When("execute operation that always fails")
    public void executeAlwaysFailingOperation() {
        var config = createRetryConfig();

        var throwable = catchThrowable(() ->
                RetryUtils.getWithRetry(config,
                        (RetryUtils.Context context) -> {
                            attemptCount.incrementAndGet();
                            throw new IllegalStateException("Always fails");
                        },
                        this::recordLoggerInvocation
                ));

        textWorld.setLastException(throwable);
    }

    /**
     * Executes an operation that always fails and interrupts its own thread on the second invocation, so the interrupt
     * status is already set when the post-failure sleep begins - retries must abort immediately instead of
     * busy-retrying with zero delay.
     */
    @When("execute operation that fails and interrupts the thread on the second invocation")
    public void executeOperationInterruptingOnSecondInvocation() {
        var config = createRetryConfig();

        var throwable = catchThrowable(() ->
                RetryUtils.getWithRetry(config,
                        (RetryUtils.Context context) -> {
                            if (attemptCount.incrementAndGet() == 2) {
                                // set BEFORE the next sleep, which must then abort immediately
                                Thread.currentThread().interrupt();
                            }

                            throw new IllegalStateException("Always fails");
                        },
                        this::recordLoggerInvocation
                ));

        textWorld.setLastException(throwable);
    }

    @When("execute operation that succeeds immediately")
    public void executeImmediatelySucceedingOperation() {
        var config = createRetryConfig();

        var throwable = catchThrowable(() ->
                result = RetryUtils.getWithRetry(config,
                        (RetryUtils.Context context) -> {
                            attemptCount.incrementAndGet();
                            return "Success";
                        },
                        this::recordLoggerInvocation
                ));

        textWorld.setLastException(throwable);
    }

    @Then("operation should complete successfully")
    public void verifySuccessfulOperation() {
        assertThat(textWorld.getLastException())
                .as("last exception")
                .isNull();

        assertThat(result)
                .as("operation result")
                .isEqualTo("Success");
    }

    @Then("operation should fail with exception")
    public void verifyFailedOperation() {
        assertThat(textWorld.getLastException())
                .as("last exception")
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Always fails");
    }

    @Then("total number of code invocations should be {int}")
    public void verifyRetryAttemptCount(int expectedAttempts) {
        assertThat(attemptCount.get())
                .as("Total retry attempts")
                .isEqualTo(expectedAttempts);
    }

    @Then("the exception logger should have been invoked {int} times")
    public void verifyLoggerInvocationCount(int expectedInvocations) {
        assertThat(loggerInvocationCount)
                .as("Exception logger invocations")
                .isEqualTo(expectedInvocations);
    }

    @Then("the last logger invocation should report {int} retries made")
    public void verifyLastLoggerRetriesMade(int expectedRetriesMade) {
        assertThat(lastLoggerRetriesMade)
                .as("Retries made reported to the exception logger on the last invocation")
                .isEqualTo(expectedRetriesMade);
    }

    @Then("the retry should be aborted by interruption")
    public void verifyRetryAbortedByInterruption() {
        assertThat(textWorld.getLastException())
                .as("last exception")
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Retry interrupted, aborting");
    }

    @Then("the interrupt flag should be set")
    public void verifyInterruptFlag() {
        // Thread.interrupted() checks AND clears the flag, leaving the thread clean for the next scenario
        assertThat(Thread.interrupted())
                .as("Interrupt flag after aborted retry")
                .isTrue();
    }

    /**
     * Records an exception logger invocation and the retry counter value the logger observed.
     *
     * @param e       exception from the failed attempt
     * @param context retry context adjusted for the next attempt
     */
    private void recordLoggerInvocation(Exception e, RetryUtils.Context context) {
        loggerInvocationCount++;
        lastLoggerRetriesMade = context.getRetriesMade();
    }

    private RetryConfig createRetryConfig() {
        return new RetryConfig() {

            @Override
            public float getGrowthFactor() {
                return 1.5F;
            }

            @Nonnull
            @Override
            public Duration getBaseDelay() {
                return baseDelay;
            }

            @Override
            public int getMaxRetryAttempts() {
                return maxRetryAttempts;
            }

        };

    }
}
