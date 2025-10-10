package guru.nicks.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

/**
 * To be passed to {@link Caffeine#expireAfter(Expiry)} to configure individual per-entry expiration (or, for brevity,
 * use {@link #createCaffeineBuilder(Function)} instead). Specifically, each cache entry will have its expiration time
 * (after creation or the most recent update) set to {@link #getExpirationInstant(Object, Object)}.
 *
 * @param <K> cache key type
 * @param <V> cache value type
 */
@FunctionalInterface
public interface CaffeineEntryExpirationCondition<K, V> extends Expiry<K, V> {

    /**
     * Configures Caffeine to use this condition (a shortcut to
     * {@code Caffeine.newBuilder().expireAfter(Expiry .writing(...))}).
     *
     * @param expirationInstantGetter expiration instant getter (should return {@code null} for no expiration)
     * @return Caffeine builder
     */
    static <K, V> Caffeine<K, V> createCaffeineBuilder(Function<? super V, Instant> expirationInstantGetter) {
        return Caffeine
                .newBuilder()
                .expireAfter(Expiry.writing((key, value) ->
                        Optional.ofNullable(expirationInstantGetter.apply(value))
                                .map(instant -> Duration.between(Instant.now(), instant))
                                // stored as package-level MAX_DURATION variable in Caffeine class
                                .orElseGet(() -> Duration.ofNanos(Long.MAX_VALUE))));
    }

    /**
     * Defines expiration time (after creation/update but not after read) for a cache entry.
     *
     * @param key   cache key
     * @param value cache value
     * @return optional expiration instant
     */
    Optional<Instant> getExpirationInstant(K key, V value);

    /**
     * Calls {@link #getExpirationInstant(Object, Object)} and, if the value is not null, calculates the number of
     * nanoseconds left until expiration.
     *
     * @param key                       cache key
     * @param value                     cache value
     * @param nanosElapsedSinceJmvStart {@link System#nanoTime()}, i.e. nanoseconds since JVM start
     * @return nanos until expiration, or {@link Long#MAX_VALUE} if no expiration
     */
    default long expireAfterCreate(K key, V value, long nanosElapsedSinceJmvStart) {
        return getExpirationInstant(key, value)
                .map(expirationInstant -> {
                    try {
                        return Duration
                                .between(Instant.now(), expirationInstant)
                                .toNanos();
                    }
                    // ArithmeticException if Duration capacity is exceeded or Long can't accommodate the nanoseconds
                    catch (RuntimeException e) {
                        return Long.MAX_VALUE;
                    }
                })
                // in Caffeine, this effectively means no expiration
                .orElse(Long.MAX_VALUE);
    }

    default long expireAfterUpdate(K key, V value, long nanosElapsedSinceJmvStart, long currentTtlNanos) {
        return expireAfterCreate(key, value, nanosElapsedSinceJmvStart);
    }

    default long expireAfterRead(K key, V value, long nanosElapsedSinceJmvStart, long currentTtlNanos) {
        return currentTtlNanos;
    }

}
