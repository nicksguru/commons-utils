package guru.nicks.cache;

import guru.nicks.utils.TimeUtils;

import jakarta.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wraps {@link #refresh()} in a {@link #getRefreshTimeout() timeout} and auto-closes {@link #getCacheRefresherTask()}
 * on shutdown (with a {@link #getShutdownTimeout() timeout}, after that {@link ExecutorService#shutdownNow()} is
 * called).
 */
public interface AsyncCacheRefresher<T> extends AutoCloseable {

    /**
     * @see #getRefreshTimeout()
     */
    Duration DEFAULT_REFRESH_TIMEOUT = Duration.ofSeconds(120);

    /**
     * @see #getShutdownTimeout()
     */
    Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(20);

    /**
     * @see #getAsyncRefreshTtlPercent()
     */
    int DEFAULT_ASYNC_REFRESH_TTL_PERCENT = 80;

    /**
     * Called by {@link #refresh()} to create and run a future that refreshes the cache.
     *
     * @return future
     */
    CompletableFuture<T> createCacheRefreshFuture();

    /**
     * Called by {@link #close()} to shut down this async task.
     *
     * @return executor service
     */
    ExecutorService getCacheRefresherTask();

    /**
     * Returns the {@link #refresh()} timeout in seconds.
     *
     * @return default implementation returns {@link #DEFAULT_REFRESH_TIMEOUT}
     */
    default Duration getRefreshTimeout() {
        return DEFAULT_REFRESH_TIMEOUT;
    }

    /**
     * Returns the {@link #close()} timeout in seconds.
     *
     * @return default implementation returns {@link #DEFAULT_SHUTDOWN_TIMEOUT}
     */
    default Duration getShutdownTimeout() {
        return DEFAULT_SHUTDOWN_TIMEOUT;
    }

    /**
     * This percent of cached object TTL defines when its asynchronous refresh should take place. Also needed to not
     * depend on the clock skew (each server has its own clock). Any value not within the {@code (0..100)} open range -
     * 'open' means both ends are exclusive - disables async refresh.
     *
     * @return default implementation returns {@value #DEFAULT_ASYNC_REFRESH_TTL_PERCENT}
     */
    default int getAsyncRefreshTtlPercent() {
        return DEFAULT_ASYNC_REFRESH_TTL_PERCENT;
    }

    /**
     * Calls {@link #createCacheRefreshFuture()} with a timeout.
     *
     * @throws IllegalStateException refresh timed out after {@link #getRefreshTimeout()} or failed internally
     */
    default void refresh() {
        Duration timeout = getRefreshTimeout();

        try {
            createCacheRefreshFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Refresh operation timed out after "
                    + TimeUtils.humanFormatDuration(timeout)
                    + ": "
                    + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Refresh operation interrupted: " + e.getMessage(), e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Refresh operation failed: " + e.getMessage(), e);
        }
    }

    /**
     * If {@link #getAsyncRefreshTtlPercent()} is greater than 0 and smaller than 100, calculates when the cache should
     * be refreshed preemptively (asynchronously).
     *
     * @param cacheExpirationDate expiration date of the cached object, nullable
     * @return optional instant in the future; empty if the expiration date is {@code null} or not in the future
     */
    default Optional<Instant> calculateAsyncRefreshDate(@Nullable Instant cacheExpirationDate) {
        int percent = getAsyncRefreshTtlPercent();

        if ((percent <= 0) || (percent >= 100)) {
            return Optional.empty();
        }

        var now = Instant.now();

        return Optional.ofNullable(cacheExpirationDate)
                .filter(expirationDate -> expirationDate.isAfter(now))
                .map(expirationDate -> Duration.between(now, expirationDate))
                .map(duration -> duration.multipliedBy(percent).dividedBy(100))
                .map(now::plus)
                // yet another check to ensure rounding didn't make the duration zero
                .filter(asyncRefreshDate -> asyncRefreshDate.isAfter(now));
    }

    /**
     * Shuts down {@link #getCacheRefresherTask()} with a timeout.
     */
    @Override
    default void close() {
        ExecutorService task = getCacheRefresherTask();
        task.shutdown();

        try {
            if (!task.awaitTermination(getShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                task.shutdownNow();
            }
        } catch (InterruptedException e) {
            task.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * If {@link #calculateAsyncRefreshDate(Instant)} returns a non-empty result (or possibly using some other logic to
     * determine when to refresh), schedules an {@link #getCacheRefresherTask() async refresher task}.
     *
     * @param expirationDate nullable expiration date of the cached object
     */
    void possiblyScheduleAsyncRefresh(@Nullable Instant expirationDate);

}
