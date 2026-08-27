package guru.nicks.commons.utils;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
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

    private static final RetryRegistry RETRY_REGISTRY = RetryRegistry.ofDefaults();
    private static final CircuitBreakerRegistry CIRCUIT_BREAKER_REGISTRY = CircuitBreakerRegistry.ofDefaults();
    private static final RateLimiterRegistry RATE_LIMITER_REGISTRY = RateLimiterRegistry.ofDefaults();

    /**
     * Creates a retrier with default settings. For defaults, see {@link RetryConfig}. If all retries fail, the original
     * exception is re-thrown.
     *
     * @param name unique retrier name; if already used, existing retrier is returned
     */
    public Retry createDefaultRetrier(String name) {
        return RETRY_REGISTRY.retry(name);
    }

    /**
     * Creates a circuit breaker with default settings. For defaults, see {@link CircuitBreakerConfig}. If the circuit
     * breaker is not open, {@link CallNotPermittedException} is thrown.
     *
     * @param name unique circuit breaker name; if already used, existing circuit breaker is returned
     */
    public CircuitBreaker createDefaultCircuitBreaker(String name) {
        return CIRCUIT_BREAKER_REGISTRY.circuitBreaker(name);
    }

    /**
     * Creates a rate limiter with default settings. For defaults, see {@link RateLimiterConfig}. If rate limit has been
     * exceeded, {@link RequestNotPermitted} is thrown.
     *
     * @param name unique rate limiter name; if already used, existing rate limiter is returned
     */
    public RateLimiter createDefaultRateLimiter(String name) {
        return RATE_LIMITER_REGISTRY.rateLimiter(name);
    }

}
