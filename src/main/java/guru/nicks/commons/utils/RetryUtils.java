package guru.nicks.commons.utils;

import am.ik.yavi.meta.ConstraintArguments;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Retries code with exponential backoff. Example usage:
 * <pre>
 *  RetryUtils.retry(
 *      Duration.ofSeconds(10), // delay until the first retry
 *      3,                      // max. retries (the first attempt is not a retry, so the total no. of attempts is 4)
 *      () -> doSomething(),    // code to run
 *                              // exception logger
 *      (e, context) -> log.error("Attempt #{} failed: {}", context.getRetriesMade(), e.getMessage(), e)
 *  );
 *  </pre>
 */
@UtilityClass
@Slf4j
public class RetryUtils {

    /**
     * Executes the given code with exponential backoff in case of errors. The math is:
     * {@code nextDelay = baseDelay * pow(growthFactor, retriesMade)}, where {@code retriesMade} starts with 0.
     *
     * @param config          retry configuration
     * @param code            code to execute
     * @param exceptionLogger Code to invoke on exception after each failed attempt. Exceptions occurred inside
     *                        {@code exceptionLogger} are logged and never propagated. When called after the very first
     *                        attempt, {@link Context#getRetriesMade()} is 1.
     * @param <T>             result type
     * @return result of successful code execution (if the number of attempts was exceeded, the last exception from
     *         {@code code} is propagated)
     */
    @SneakyThrows
    public static <T> T getWithRetry(RetryConfig config, Function<Context, T> code,
            BiConsumer<Exception, Context> exceptionLogger) {
        checkNotNull(code, "code");
        checkNotNull(exceptionLogger, "exceptionHandler");

        Context context = createContext(config);
        Exception lastException = null;

        // start with 0 which is not a retry yet
        for (int retryAttempt = 0; retryAttempt <= context.getMaxRetryAttempts(); retryAttempt++) {
            try {
                return code.apply(context);
            } catch (Exception e) {
                lastException = e;
                context = adjustForNextRetry(context, lastException);

                try {
                    exceptionLogger.accept(e, context);
                } catch (Exception fromLogger) {
                    log.error("Error in exception logger (ignored): {}", fromLogger.getMessage(), fromLogger);
                }

                sleep(context);
            }
        }

        throw (lastException == null)
                ? new IllegalStateException("No attempt was made")
                : lastException;
    }

    /**
     * Creates context with the given base delay.
     *
     * @param config retry configuration
     * @return context
     */
    @ConstraintArguments
    private static Context createContext(RetryConfig config) {
        checkNotNull(config, _RetryUtilsCreateContextArgumentsMeta.CONFIG.name());
        checkNotNull(config.getBaseDelay(), "baseDelay");

        if (config.getBaseDelay().isNegative()) {
            throw new IllegalArgumentException("Base delay must not be negative");
        }

        if (config.getMaxRetryAttempts() < 0) {
            throw new IllegalArgumentException("Max. retry attempts must not be negative");
        }

        if (config.getGrowthFactor() <= 0.0) {
            throw new IllegalArgumentException("Factor must be positive");
        }

        return Context.builder()
                .maxRetryAttempts(config.getMaxRetryAttempts())
                .retriesMade(0)
                .growthFactor(config.getGrowthFactor())
                .baseDelay(config.getBaseDelay())
                .nextDelay(config.getBaseDelay())
                .build();
    }

    /**
     * Creates context with {@link Context#getRetriesMade()} and {@link Context#getNextDelay()} adjusted. Re-throws
     * {@code lastException} if the number of retries has been exceeded.
     *
     * @param context context for the previous attempt
     * @param e       exception from the previous attempt
     * @return context for the next attempt
     */
    @SneakyThrows
    @ConstraintArguments
    private static Context adjustForNextRetry(RetryUtils.Context context, Exception e) {
        checkNotNull(context, _RetryUtilsAdjustForNextRetryArgumentsMeta.CONTEXT.name());
        // starts with 0 because the very first attempt is not a retry
        int retriesMade = context.getRetriesMade();

        if (retriesMade >= context.getMaxRetryAttempts()) {
            log.error("Number of attempts reached maximum ({}) after exception: {}", context.getMaxRetryAttempts(),
                    e.getMessage(), e);
            throw e;
        }

        long nextDelayMillis = Math.round(
                context.getBaseDelay().toMillis() * Math.pow(context.getGrowthFactor(), retriesMade));

        Context newContext = context.toBuilder()
                .retriesMade(retriesMade + 1)
                .nextDelay(Duration.ofMillis(nextDelayMillis))
                .build();

        log.debug("Next backoff: {} -> {}", context, newContext);
        return newContext;
    }

    @ConstraintArguments
    private static void sleep(RetryUtils.Context context) {
        checkNotNull(context, _RetryUtilsSleepArgumentsMeta.CONTEXT.name());

        try {
            Thread.sleep(context.getNextDelay());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Value
    @NonFinal
    @Builder(toBuilder = true)
    public static class Context {

        float growthFactor;

        /**
         * 0 means the first attempt (which is not a retry), 1 means the first retry, etc.
         */
        int retriesMade;

        /**
         * 0 means no retries are allowed
         */
        int maxRetryAttempts;

        Duration baseDelay;

        /**
         * During the first attempt (which is not a retry), equals {@link #getBaseDelay()}.
         */
        Duration nextDelay;

    }

}
