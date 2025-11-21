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

    @Given("base delay is {int} ms")
    public void setBaseDelay(int milliseconds) {
        this.baseDelay = Duration.ofMillis(milliseconds);
    }

    @Given("maximum {int} retry attempts")
    public void setMaxRetryAttempts(int attempts) {
        this.maxRetryAttempts = attempts;
    }

    @When("execute operation that succeeds after {int} invocations")
    public void executeOperationWithFailures(int succeedAfterInvocations) {
        var config = createRetryConfig();

        var throwable = catchThrowable(() ->
                result = RetryUtils.getWithRetry(config,
                        (RetryUtils.Context context) -> {
                            if (attemptCount.incrementAndGet() < succeedAfterInvocations) {
                                throw new RuntimeException("Planned failure");
                            }

                            return "Success";
                        },
                        (e, context) -> {}
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
                        (e, context) -> {}
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
                        (e, context) -> {}
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
