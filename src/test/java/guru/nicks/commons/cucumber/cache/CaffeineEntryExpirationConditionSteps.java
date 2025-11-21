package guru.nicks.commons.cucumber.cache;

import guru.nicks.commons.cache.CaffeineEntryExpirationCondition;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing a concrete implementation of {@link CaffeineEntryExpirationCondition}.
 */
public class CaffeineEntryExpirationConditionSteps {

    private Caffeine<String, TestExpiringValue> caffeineBuilder;
    private LoadingCache<String, TestExpiringValue> cache;
    private TestExpiringValue cachedValue;

    @When("a Caffeine builder is created with the implementation")
    public void aCaffeineBuilderIsCreatedWithTheImplementation() {
        Function<TestExpiringValue, Instant> expirationDateGetter = TestExpiringValue::getExpirationDate;
        caffeineBuilder = CaffeineEntryExpirationCondition.createCaffeineBuilder(expirationDateGetter);
    }

    @When("a cache is built from the builder")
    public void aCacheIsBuiltFromTheBuilder() {
        cache = caffeineBuilder.build(key ->
                TestExpiringValue.builder()
                        .value("Value for " + key)
                        .expirationDate(Instant.now().plus(1, ChronoUnit.MINUTES))
                        .build());
    }

    @When("a value is loaded into the cache")
    public void aValueIsLoadedIntoTheCache() {
        cachedValue = cache.get("testKey");
    }

    @Then("the value should be available in the cache")
    public void theValueShouldBeAvailableInTheCache() {
        assertThat(cachedValue)
                .as("cachedValue")
                .isNotNull();

        assertThat(cachedValue.getValue())
                .as("cachedValue.value")
                .isEqualTo("Value for testKey");
    }

    @When("the cache is accessed after the expiration time")
    public void theCacheIsAccessedAfterTheExpirationTime() throws InterruptedException {
        // for testing purposes, use a very short expiration time
        TestExpiringValue shortLivedValue = TestExpiringValue.builder()
                .value("Short lived")
                .expirationDate(Instant.now().plus(1, ChronoUnit.MILLIS))
                .build();

        cache.put("shortLived", shortLivedValue);
        // wait for expiration
        TimeUnit.MILLISECONDS.sleep(10);

        // try to access the value
        cachedValue = cache.getIfPresent("shortLived");
    }

    @Then("the value should be expired from the cache")
    public void theValueShouldBeExpiredFromTheCache() {
        assertThat(cachedValue)
                .as("cachedValue")
                .isNull();
    }

    /**
     * A test class that holds a value and its expiration date.
     */
    @Value
    @Builder
    private static class TestExpiringValue {

        String value;
        Instant expirationDate;

    }

}
