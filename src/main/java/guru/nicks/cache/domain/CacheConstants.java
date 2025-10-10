package guru.nicks.cache.domain;

import lombok.experimental.UtilityClass;
import org.springframework.cache.annotation.Cacheable;

@UtilityClass
public class CacheConstants {

    /**
     * If not sure about the cache capacity, use this value as a starting point.
     */
    public static final int DEFAULT_CAFFEINE_CACHE_CAPACITY = 300;

    /**
     * Redis topic separator recognized by UI tools that represent caches as collapsible trees.
     */
    public static final String TOPIC_DELIMITER = "::";

    /**
     * Hardcoded in Spring.
     */
    public static final String DEFAULT_CACHE_MANAGER_BEAN = "cacheManager";

    public static final String MEMORY_CACHE_MANAGER_PREFIX = "memory-";
    public static final String PERSISTENT_CACHE_MANAGER_PREFIX = "persistent-";

    /**
     * The 'minutes[s]/hours[s]/days[s]' prefixes cannot be changed - they're parsed  to construct durations,
     */
    public static final String HOURS_SUFFIX = "hours";
    public static final String MINUTES_SUFFIX = "minutes";
    public static final String DAYS_SUFFIX = "days";

    /**
     * Appended to {@value #MEMORY_CACHE_MANAGER_PREFIX} and {@value #PERSISTENT_CACHE_MANAGER_PREFIX}. Hardcoded for
     * consistent references from {@link Cacheable#cacheManager()}.
     * <p>
     * WARNING: each TTL must be referenced in the code that creates cache managers for all TTLs listed here. Omitted
     * values will not be applicable.
     */
    public static final String TTL_1MIN = "1" + MINUTES_SUFFIX;
    public static final String TTL_2MIN = "2" + MINUTES_SUFFIX;
    public static final String TTL_3MIN = "3" + MINUTES_SUFFIX;
    public static final String TTL_5MIN = "5" + MINUTES_SUFFIX;
    public static final String TTL_10MIN = "10" + MINUTES_SUFFIX;
    public static final String TTL_15MIN = "15" + MINUTES_SUFFIX;
    public static final String TTL_20MIN = "20" + MINUTES_SUFFIX;
    public static final String TTL_30MIN = "30" + MINUTES_SUFFIX;

    public static final String TTL_1HR = "1" + HOURS_SUFFIX;
    public static final String TTL_2HR = "2" + HOURS_SUFFIX;
    public static final String TTL_4HR = "4" + HOURS_SUFFIX;
    public static final String TTL_6HR = "6" + HOURS_SUFFIX;
    public static final String TTL_8HR = "8" + HOURS_SUFFIX;
    public static final String TTL_12HR = "12" + HOURS_SUFFIX;
    public static final String TTL_24HR = "24" + HOURS_SUFFIX;

    public static final String TTL_1D = "1" + DAYS_SUFFIX;
    public static final String TTL_3D = "3" + DAYS_SUFFIX;
    public static final String TTL_7D = "7" + DAYS_SUFFIX;
    public static final String TTL_14D = "14" + DAYS_SUFFIX;
    public static final String TTL_30D = "30" + DAYS_SUFFIX;
    public static final String DEFAULT_TTL = TTL_30D;
    public static final String TTL_90D = "90" + DAYS_SUFFIX;
    public static final String TTL_365D = "365" + DAYS_SUFFIX;

}
