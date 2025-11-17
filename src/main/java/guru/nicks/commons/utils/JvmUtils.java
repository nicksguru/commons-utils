package guru.nicks.commons.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import lombok.experimental.UtilityClass;
import org.springframework.util.unit.DataSize;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for accessing JVM memory information with caching support.
 * <p>
 * This class is thread-safe. The internal cache is implemented using Caffeine, which provides thread-safe operations.
 */
@UtilityClass
public class JvmUtils {

    public static final int CACHE_TTL_SECONDS = 120;

    private static final Cache<String, DataSize> CACHE = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_TTL_SECONDS, TimeUnit.SECONDS)
            .build();

    /**
     * Returns the maximum memory available to the JVM. Theoretically, this value may change during runtime. The result
     * is cached for {@value #CACHE_TTL_SECONDS} seconds because system calls are expensive.
     * <p>
     * If no {@code -Xmx} JVM flag is set, this method returns the total memory instead of {@link Long#MAX_VALUE}.
     *
     * @return max memory available to JVM
     */
    public static DataSize getMaxMemory() {
        // 'get' method may return null as per Caffeine specs, but never does in this particular case
        //noinspection DataFlowIssue
        return CACHE.get("maxMemory", key -> {
            long bytes = DirectMemoryAccessor.getMaxMemoryBytes();

            // max. long means there's no '-Xmx' JVM flag set, in which case total memory is the same as max. memory
            if (bytes == Long.MAX_VALUE) {
                bytes = DirectMemoryAccessor.getTotalMemoryBytes();
            }

            return DataSize.ofBytes(bytes);
        });
    }

    /**
     * The result is cached for {@value #CACHE_TTL_SECONDS} seconds because system calls are expensive.
     *
     * @return free memory available to JVM
     */
    public static DataSize getFreeMemory() {
        // 'get' method may return null as per Caffeine specs, but never does in this particular case
        //noinspection DataFlowIssue
        return CACHE.get("freeMemory", key -> DataSize.ofBytes(DirectMemoryAccessor.getFreeMemoryBytes()));
    }

    /**
     * Invalidates all cached memory values, forcing fresh retrieval on next access.
     * <p>
     * This method clears the internal cache that stores JVM memory information. After calling this method, subsequent
     * calls to {@link #getMaxMemory()} and {@link #getFreeMemory()} will perform fresh system calls to retrieve current
     * memory values instead of returning cached results.
     * <p>
     * This method primarily exists for testing purposes - to test cache expiration conditions.
     */
    public static void invalidateCache() {
        CACHE.invalidateAll();
    }

    /**
     * Encapsulates direct {@link Runtime} calls. Bypasses cache and exists primarily for testing purposes, as system
     * calls are expensive.
     */
    @UtilityClass
    @VisibleForTesting
    public static class DirectMemoryAccessor {

        public static long getTotalMemoryBytes() {
            return Runtime.getRuntime().totalMemory();
        }

        public static long getMaxMemoryBytes() {
            // max. long if there's no -Xmx set
            return Runtime.getRuntime().maxMemory();
        }

        public static long getFreeMemoryBytes() {
            return Runtime.getRuntime().freeMemory();
        }

    }

}
