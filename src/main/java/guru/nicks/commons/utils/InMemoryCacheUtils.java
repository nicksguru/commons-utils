package guru.nicks.commons.utils;

import guru.nicks.commons.cache.domain.CacheConstants;
import guru.nicks.commons.cache.domain.CacheProperties;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.support.GenericApplicationContext;

import java.time.Duration;
import java.util.Collection;
import java.util.function.IntFunction;

/**
 * Creates in-memory caches with various TTLs according to {@link CacheProperties#getDurations()}.
 * <p>
 * Additionally, put/evict operations can be synchronized (via {@link CacheProperties#isTransactionAware()}) with
 * ongoing Spring-managed transactions (however, this may lead to non-obvious results, as caching is usually treated as
 * in independent feature).
 * <p>
 * WARNING: only results of public bean methods can be cached with {@link Cacheable @Cacheable} (because of proxies).
 */
@UtilityClass
@Slf4j
public class InMemoryCacheUtils {

    /**
     * Replaces the default cache manager with a custom in-memory cache manager configured according to
     * {@link CacheProperties.InMemory#getDefaultTimeToLive()}. It will be used when no explicit cache manager is
     * specified in {@link Cacheable @Cacheable} annotations.
     *
     * @param cacheProperties cache configuration properties
     * @param appContext      Spring application context where the new primary cache manager bean will be registered
     */
    public static void replaceDefaultCacheManager(CacheProperties cacheProperties,
            GenericApplicationContext appContext) {
        replacePrimaryCacheManager(cacheProperties, appContext);
    }

    /**
     * Configures additional cache managers with different time-to-live (TTL) durations.
     * <p>
     * This method creates multiple cache managers based on the configured durations for minutes, hours, and days. Each
     * cache manager is registered as a separate bean in the application context and can be referenced by name in
     * {@link Cacheable @Cacheable} annotations using the cacheManager attribute.
     * <p>
     * The created cache managers follow the naming convention defined by
     * {@link CacheConstants#MEMORY_CACHE_MANAGER_PREFIX} combined with the duration value and optional suffix from the
     * cache definition.
     *
     * @param cacheProperties cache configuration properties containing duration definitions and capacity settings
     * @param appContext      Spring application context where the cache manager beans will be registered
     */
    public static void configureAdditionalCacheManagers(CacheProperties cacheProperties,
            GenericApplicationContext appContext) {
        createMemoryCacheManagers(cacheProperties, appContext,
                cacheProperties.getDurations().getMinutes(), Duration::ofMinutes,
                cacheProperties.getInMemory().getMaxEntriesPerCacheManager());
        createMemoryCacheManagers(cacheProperties, appContext,
                cacheProperties.getDurations().getHours(), Duration::ofHours,
                cacheProperties.getInMemory().getMaxEntriesPerCacheManager());
        createMemoryCacheManagers(cacheProperties, appContext,
                cacheProperties.getDurations().getDays(), Duration::ofDays,
                cacheProperties.getInMemory().getMaxEntriesPerCacheManager());
    }

    /**
     * Creates cache managers based on {@link CacheProperties.CacheDefinition}.
     *
     * @param cacheDefinitions cache definitions
     * @param durationCreator  creates {@link Duration} out of {@link CacheProperties.CacheDefinition#getValue()}
     */
    private static void createMemoryCacheManagers(CacheProperties cacheProperties, GenericApplicationContext appContext,
            Collection<CacheProperties.CacheDefinition> cacheDefinitions,
            IntFunction<Duration> durationCreator, int maxEntriesPerCacheManager) {
        for (var cacheDefinition : cacheDefinitions) {
            var cacheManagerNameBuilder = new StringBuilder()
                    .append(CacheConstants.MEMORY_CACHE_MANAGER_PREFIX)
                    .append(cacheDefinition.getValue());

            if (StringUtils.isNotBlank(cacheDefinition.getSuffix())) {
                cacheManagerNameBuilder.append(cacheDefinition.getSuffix());
            }

            createMemoryCacheManager(cacheProperties, appContext,
                    cacheManagerNameBuilder.toString(),
                    durationCreator.apply(cacheDefinition.getValue()), maxEntriesPerCacheManager);
        }
    }

    /**
     * Creates cache manager.
     *
     * @param cacheManagerName cache manager name
     * @param ttl              cache TTL
     */
    private static void createMemoryCacheManager(CacheProperties cacheProperties, GenericApplicationContext appContext,
            String cacheManagerName, Duration ttl, int maxEntries) {
        log.info("Creating in-memory cache manager with TTL '{}' and capacity {} entries. Usage: "
                        + "@Cacheable(cacheNames = \"someCache\", key = \"#someArg\", cacheManager = \"{}\").",
                TimeUtils.humanFormatDuration(ttl), maxEntries, cacheManagerName);

        var cache = configureCache(ttl, maxEntries);
        registerCacheManagerBean(cacheProperties, appContext, cacheManagerName, cache);
    }

    private static Caffeine<Object, Object> configureCache(Duration ttl, int maxEntries) {
        return Caffeine.newBuilder()
                .maximumSize(maxEntries)
                .expireAfterWrite(ttl)
                // remove expired entries asap (not only when the cache is accessed) to save memory
                .scheduler(Scheduler.systemScheduler());
    }

    private static void registerCacheManagerBean(CacheProperties cacheProperties,
            GenericApplicationContext appContext, String beanName,
            Caffeine<Object, Object> cache, BeanDefinitionCustomizer... customizers) {
        var cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(cache);
        CacheManager bean;

        // Synchronize put/evict operations with ongoing Spring-managed transactions. Caffeine isn't transaction-aware
        // natively, so wrap it in a special proxy that handles transactions.
        if (cacheProperties.isTransactionAware()) {
            log.warn("In-memory cache is now transaction-aware - counter-intuitive side effects may arise");
            bean = new TransactionAwareCacheManagerProxy(cacheManager);
        } else {
            bean = cacheManager;
        }

        appContext.registerBean(beanName, CacheManager.class, () -> bean, customizers);
    }

    /**
     * Creates/overrides primary bean called {@link CacheConstants#DEFAULT_CACHE_MANAGER_BEAN} - needed if no explicit
     * cache manager is mentioned (as {@link Cacheable#cacheManager()}).
     */
    private static void replacePrimaryCacheManager(CacheProperties cacheProperties,
            GenericApplicationContext appContext) {
        // bean removal surprisingly calls EntityManagerFactory.close() which makes JPA stop working
        //        if (appContext.containsBeanDefinition(CacheProperties.DEFAULT_CACHE_MANAGER_BEAN)) {
        //            appContext.removeBeanDefinition(CacheProperties.DEFAULT_CACHE_MANAGER_BEAN);
        //        }

        log.info("Overriding default in-memory cache manager bean '{}' with TTL '{}' and capacity {} entries. Usage: "
                        + "@Cacheable(cacheNames = \"someCache\", key = \"#someArg\").",
                CacheConstants.DEFAULT_CACHE_MANAGER_BEAN,
                TimeUtils.humanFormatDuration(cacheProperties.getInMemory().getDefaultTimeToLive()),
                cacheProperties.getInMemory().getMaxEntriesPerCacheManager());

        var cache = configureCache(cacheProperties.getInMemory().getDefaultTimeToLive(),
                cacheProperties.getInMemory().getMaxEntriesPerCacheManager());
        registerCacheManagerBean(cacheProperties, appContext,
                CacheConstants.DEFAULT_CACHE_MANAGER_BEAN + "Overridden", cache,
                beanDef -> beanDef.setPrimary(true));
    }

}
