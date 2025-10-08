package guru.nicks.utils;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.experimental.UtilityClass;

/**
 * Creates Resilience4j artifacts for
 * <a href="https://resilience4j.readme.io/docs/getting-started#sneak-preview">programmatic use</a> in non-bean
 * classes. For Spring beans, the approach is different: Resilience4j settings should be
 * <a href="https://resilience4j.readme.io/docs/getting-started-3#configuration">stored in app config</a>, and such
 * annotations as {@code @Retry} should be leveraged.
 */
@UtilityClass
public class Resilience4jUtils {

    /**
     * Creates a retrier with default settings. By default, the delay between (3) retries is constant and equals 0.5
     * seconds - see {@link RetryConfig}.
     *
     * @param name unique retrier name; if already used, no new retrier is created
     */
    public Retry createDefaultRetrier(String name) {
        return RetryRegistry
                .ofDefaults()
                .retry(name);
    }

    /**
     * Creates a circuit breaker with default settings. For defaults, see {@link CircuitBreakerConfig}.
     *
     * @param name unique circuit breaker name; if already used, no new circuit breaker is created
     */
    public CircuitBreaker createDefaultCircuitBreaker(String name) {
        return CircuitBreakerRegistry
                .ofDefaults()
                .circuitBreaker(name);
    }

}
