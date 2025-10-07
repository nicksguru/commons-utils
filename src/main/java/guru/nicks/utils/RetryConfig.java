package guru.nicks.utils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * The formula for the maximum time elapsed (after all the attempts have been performed) is:
 * {@code baseDelay * (factor^0 + factor^1 + factor^2 + ... + factor^n)} where {@code factor^0} (i.e. 1) defines delay
 * for the first retry, etc.
 */
public interface RetryConfig {

    /**
     * @see #getGrowthFactor()
     */
    float DEFAULT_FACTOR = 1.5F;

    /**
     * @see #getBaseDelay()
     */
    Duration DEFAULT_BASE_DELAY = Duration.of(10, ChronoUnit.SECONDS);

    /**
     * @see #getMaxRetryAttempts()
     */
    int DEFAULT_MAX_RETRY_ATTEMPTS = 7;

    /**
     * Defines the factor for exponential backoff i.e. what's powered by the number of retries.
     *
     * @return default implementation returns {@value #DEFAULT_FACTOR}
     */
    default float getGrowthFactor() {
        return DEFAULT_FACTOR;
    }

    /**
     * Returns base delay for exponential backoff.
     *
     * @return default implementation returns {@link #DEFAULT_BASE_DELAY}
     */
    default Duration getBaseDelay() {
        return DEFAULT_BASE_DELAY;
    }

    /**
     * Defines the max. number of retries obtaining a fresh header value. This value doesn't include the initial attempt
     * which is not a retry. Therefore, returning 0 means no retries (only one attempt).
     * <p>
     * For example, if 7 is returned, with base retry delay of 10 seconds and factor of 1.5, the maximum time elapsed
     * (after all the attempts have been performed) is {@code 10 + 10*1.5 + 10*1.5^2 + ... + 10*1.5^6} = 5.3 minutes.
     *
     * @return default implementation returns {@value #DEFAULT_MAX_RETRY_ATTEMPTS}
     */
    default int getMaxRetryAttempts() {
        return DEFAULT_MAX_RETRY_ATTEMPTS;
    }

}
