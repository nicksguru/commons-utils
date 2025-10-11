package guru.nicks.cache.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ConfigurationProperties(prefix = "cache")
@Validated
// immutability
@Value
@NonFinal // needed for CGLIB to bind property values (nested classes don't need this)
@Jacksonized
@Builder(toBuilder = true)
public class CacheProperties {

    /**
     * If {@code true}, cache managers having native transaction awareness (such as Redis) will have it enabled, and
     * those having no this feature (such as in-memory cache manager) will be wrapped in
     * {@code TransactionAwareCacheManagerProxy}. This causes counter-intuitive side effects in
     * {@code @Cacheable / @CachePut / @CacheEvict}: results of, for example, remote calls and heavy computations will
     * not be cached until parent transactions are committed. But caching is usually needed as a low-level feature
     * working independently.
     */
    boolean transactionAware;

    @NotNull
    @Valid // not validated by Spring Boot 3.4 without this
    InMemory inMemory;

    @NotNull
    @Valid // not validated by Spring Boot 3.4 without this
    Durations durations = new Durations();

    @Value
    @Jacksonized
    @Builder(toBuilder = true)
    public static class Durations {

        /**
         * Hardcoded for consistent references from {@link Cacheable#cacheManager()}.
         */
        List<CacheDefinition> minutes = CacheDefinition.of(
                CacheConstants.TTL_1MIN,
                CacheConstants.TTL_2MIN,
                CacheConstants.TTL_3MIN,
                CacheConstants.TTL_5MIN,
                CacheConstants.TTL_10MIN,
                CacheConstants.TTL_15MIN,
                CacheConstants.TTL_20MIN,
                CacheConstants.TTL_30MIN);

        List<CacheDefinition> hours = CacheDefinition.of(
                CacheConstants.TTL_1HR,
                CacheConstants.TTL_2HR,
                CacheConstants.TTL_4HR,
                CacheConstants.TTL_6HR,
                CacheConstants.TTL_8HR,
                CacheConstants.TTL_12HR,
                CacheConstants.TTL_24HR);

        List<CacheDefinition> days = CacheDefinition.of(
                CacheConstants.TTL_1D,
                CacheConstants.TTL_3D,
                CacheConstants.TTL_7D,
                CacheConstants.TTL_14D,
                CacheConstants.TTL_30D,
                CacheConstants.TTL_90D,
                CacheConstants.TTL_365D);

    }

    @Value
    @Jacksonized
    @Builder(toBuilder = true)
    public static class InMemory {

        /**
         * Hardcoded (30 days) in order to reliably refer to an <b>existing</b> TTL found in {@link CacheConstants}.
         */
        @NotNull
        Duration defaultTimeToLive = Duration.ofDays(
                CacheDefinition.of(CacheConstants.DEFAULT_TTL)
                        .getFirst()
                        .getValue());

        /**
         * Each cache manager corresponds to a certain TTL.
         */
        @Positive
        @NotNull
        int maxEntriesPerCacheManager;

    }

    @Value
    @Jacksonized
    @Builder(toBuilder = true)
    public static class CacheDefinition {

        /**
         * $1: duration value; $2: suffix (minutes/hours/days)
         */
        private static final Pattern SPLIT_DURATION_AND_SUFFIX = Pattern.compile(
                "(?xi)^\\s* "
                        + "(\\d+)"
                        // suffix (time unit)
                        + "("
                        + Pattern.quote(CacheConstants.MINUTES_SUFFIX)
                        + "|"
                        + Pattern.quote(CacheConstants.HOURS_SUFFIX)
                        + "|"
                        + Pattern.quote(CacheConstants.DAYS_SUFFIX)
                        + ") \\s*$");

        @Positive
        int value;

        @NotBlank
        String suffix;

        /**
         * Parses {@code 123minutes/hours/days} into duration and suffix.
         *
         * @param definitions strings in the above format
         * @return parsed definitions (blank strings are skipped)
         * @throws IllegalArgumentException unparseable input
         */
        public static List<CacheDefinition> of(String... definitions) {
            // skip blank definitions
            List<String> nonEmptyDefinitions = Arrays.stream(definitions)
                    .filter(StringUtils::isNotBlank)
                    .toList();

            List<CacheDefinition> cacheDefinitions = nonEmptyDefinitions.stream()
                    .map(SPLIT_DURATION_AND_SUFFIX::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> CacheDefinition.builder()
                            .value(Integer.parseUnsignedInt(matcher.group(1)))
                            .suffix(matcher.group(2))
                            .build())
                    .toList();

            if (cacheDefinitions.size() != nonEmptyDefinitions.size()) {
                throw new IllegalArgumentException("Failed to parse cache definitions: "
                        + Arrays.toString(definitions));
            }

            return cacheDefinitions;
        }

    }

}
