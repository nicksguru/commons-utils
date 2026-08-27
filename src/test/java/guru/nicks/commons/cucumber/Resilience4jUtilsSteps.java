package guru.nicks.commons.cucumber;

import guru.nicks.commons.utils.Resilience4jUtils;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing {@link Resilience4jUtils} functionality: same-name artifacts must be backed by one
 * shared registry so that state (metrics, breaker status, rate limit windows) accumulates across calls.
 */
public class Resilience4jUtilsSteps {

    /**
     * Number of successful executions used to prove that metrics accumulate on a shared instance.
     */
    private static final int SUCCESSFUL_EXECUTIONS = 2;

    private Retry firstRetrier;
    private Retry secondRetrier;
    private CircuitBreaker firstCircuitBreaker;
    private CircuitBreaker secondCircuitBreaker;
    private RateLimiter firstRateLimiter;
    private RateLimiter secondRateLimiter;

    @When("two default retriers are created with the same name {string}")
    public void twoDefaultRetriersAreCreatedWithTheSameName(String name) {
        firstRetrier = Resilience4jUtils.createDefaultRetrier(name);
        secondRetrier = Resilience4jUtils.createDefaultRetrier(name);
    }

    @When("two default circuit breakers are created with the same name {string}")
    public void twoDefaultCircuitBreakersAreCreatedWithTheSameName(String name) {
        firstCircuitBreaker = Resilience4jUtils.createDefaultCircuitBreaker(name);
        secondCircuitBreaker = Resilience4jUtils.createDefaultCircuitBreaker(name);
    }

    @When("two default rate limiters are created with the same name {string}")
    public void twoDefaultRateLimitersAreCreatedWithTheSameName(String name) {
        firstRateLimiter = Resilience4jUtils.createDefaultRateLimiter(name);
        secondRateLimiter = Resilience4jUtils.createDefaultRateLimiter(name);
    }

    @Then("the retriers should be the same instance")
    public void theRetriersShouldBeTheSameInstance() {
        assertThat(secondRetrier)
                .as("secondRetrier")
                .isSameAs(firstRetrier);
    }

    @Then("the retrier metrics should accumulate across successful executions")
    public void theRetrierMetricsShouldAccumulateAcrossSuccessfulExecutions() {
        for (int i = 0; i < SUCCESSFUL_EXECUTIONS; i++) {
            firstRetrier.executeSupplier(() -> "ok");
        }

        // metrics read via the second reference must reflect executions performed on the first one
        assertThat(secondRetrier.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt())
                .as("successful calls without retry")
                .isEqualTo(SUCCESSFUL_EXECUTIONS);
    }

    @Then("the circuit breakers should be the same instance")
    public void theCircuitBreakersShouldBeTheSameInstance() {
        assertThat(secondCircuitBreaker)
                .as("secondCircuitBreaker")
                .isSameAs(firstCircuitBreaker);
    }

    @Then("the circuit breaker metrics should accumulate across successful executions")
    public void theCircuitBreakerMetricsShouldAccumulateAcrossSuccessfulExecutions() {
        for (int i = 0; i < SUCCESSFUL_EXECUTIONS; i++) {
            firstCircuitBreaker.executeSupplier(() -> "ok");
        }

        // metrics read via the second reference must reflect executions performed on the first one
        assertThat(secondCircuitBreaker.getMetrics().getNumberOfSuccessfulCalls())
                .as("successful calls")
                .isEqualTo(SUCCESSFUL_EXECUTIONS);
    }

    @Then("the rate limiters should be the same instance")
    public void theRateLimitersShouldBeTheSameInstance() {
        assertThat(secondRateLimiter)
                .as("secondRateLimiter")
                .isSameAs(firstRateLimiter);
    }

}
