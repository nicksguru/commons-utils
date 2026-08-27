package guru.nicks.commons.cucumber;

import guru.nicks.commons.ApplicationContextHolder;
import guru.nicks.commons.utils.ResourceUtils;
import guru.nicks.commons.utils.crypto.ChecksumUtils;

import com.github.benmanes.caffeine.cache.Cache;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.SneakyThrows;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Cucumber step definitions for testing {@link ResourceUtils#getAppBuildTag()}.
 */
public class ResourceUtilsSteps {

    @Mock
    private ApplicationContext mockApplicationContext;
    @Mock
    private GitProperties mockGitProperties;
    @Mock
    private BuildProperties mockBuildProperties;
    private AutoCloseable closeableMocks;

    private String firstBuildTag;
    private String secondBuildTag;
    private String actualBuildTag;
    private int concurrentThreadCount;
    private Queue<ResourceUtils.CacheEntry> concurrentEntries;
    private Queue<Throwable> concurrentFailures;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        ApplicationContextHolder.setApplicationContext(mockApplicationContext);
    }

    @After
    public void afterEachScenario() throws Exception {
        // clear build tag cache
        Field cacheField = ResourceUtils.class.getDeclaredField("BUILD_TAG_CACHE");
        cacheField.setAccessible(true);
        Cache<?, ?> cache = (Cache<?, ?>) cacheField.get(null);
        cache.invalidateAll();

        ApplicationContextHolder.setApplicationContext(null);
        closeableMocks.close();
    }

    @Given("application context has GitProperties with commit ID {string}")
    public void applicationContextHasGitPropertiesWithCommitId(String commitId) {
        when(mockApplicationContext.getBean(GitProperties.class))
                .thenReturn(mockGitProperties);

        when(mockGitProperties.getCommitId())
                .thenReturn(commitId);
    }

    @Given("application context has GitProperties with empty commit ID")
    public void applicationContextHasGitPropertiesWithEmptyCommitId() {
        when(mockApplicationContext.getBean(GitProperties.class))
                .thenReturn(mockGitProperties);

        when(mockGitProperties.getCommitId())
                .thenReturn("");
    }

    @Given("application context has no GitProperties bean")
    public void applicationContextHasNoGitPropertiesBean() {
        when(mockApplicationContext.getBean(GitProperties.class))
                .thenThrow(new NoSuchBeanDefinitionException(GitProperties.class));
    }

    @Given("application context has BuildProperties with time {string}")
    public void applicationContextHasBuildPropertiesWithTime(String buildTimeIso8601) {
        when(mockApplicationContext.getBean(BuildProperties.class))
                .thenReturn(mockBuildProperties);

        Instant instant = Instant.parse(buildTimeIso8601);
        when(mockBuildProperties.getTime())
                .thenReturn(instant);
    }

    @Given("application context has BuildProperties with null time")
    public void applicationContextHasBuildPropertiesWithNullTime() {
        when(mockApplicationContext.getBean(BuildProperties.class))
                .thenReturn(mockBuildProperties);

        when(mockBuildProperties.getTime())
                .thenReturn(null);
    }

    @Given("application context has no BuildProperties bean")
    public void applicationContextHasNoBuildPropertiesBean() {
        when(mockApplicationContext.getBean(BuildProperties.class))
                .thenThrow(new NoSuchBeanDefinitionException(BuildProperties.class));
    }

    @When("app build tag is retrieved first time")
    public void appBuildTagIsRetrievedFirstTime() {
        firstBuildTag = ResourceUtils.getAppBuildTag();
    }

    @And("app build tag is retrieved second time")
    public void appBuildTagIsRetrievedSecondTime() {
        secondBuildTag = ResourceUtils.getAppBuildTag();
    }

    @When("app build tag is retrieved")
    public void appBuildTagIsRetrieved() {
        actualBuildTag = ResourceUtils.getAppBuildTag();
    }

    @Then("build tag should be a checksum of {string}")
    public void buildTagShouldBeAChecksumOf(String expectedInput) {
        String expectedChecksum = ChecksumUtils.computeJsonChecksum(expectedInput);

        assertThat(actualBuildTag)
                .as("Build tag should be checksum of '%s'", expectedInput)
                .isEqualTo(expectedChecksum);
    }

    @Then("build tag should be a checksum of current time")
    public void buildTagShouldBeAChecksumOfCurrentTime() {
        // The build tag should be a checksum of some timestamp string.
        // We can't predict the exact value, but we can verify it's a valid checksum format.
        assertThat(actualBuildTag)
                .as("Build tag should be a valid Base64 checksum")
                .isNotNull()
                .isNotEmpty()
                .matches("^[A-Za-z0-9+/]+=*$");
    }

    @Then("build tag should be cached")
    public void buildTagShouldBeCached() {
        String secondCall = ResourceUtils.getAppBuildTag();

        assertThat(secondCall)
                .as("Build tag should be cached")
                .isEqualTo(actualBuildTag);
    }

    @Then("both build tags should be identical")
    public void bothBuildTagsShouldBeIdentical() {
        assertThat(firstBuildTag)
                .as("First build tag")
                .isNotNull();

        assertThat(secondBuildTag)
                .as("Second build tag")
                .isEqualTo(firstBuildTag);
    }

    /**
     * Retrieves the same classpath resource from the given number of virtual threads, all released simultaneously by a
     * barrier, so cache misses overlap and the lock-free loading path is exercised.
     *
     * @param threadCount number of threads to run
     * @param path        resource path - passed from app config and is trusted
     * @param filename    - passed from web requests and is therefore NOT trusted
     */
    @SneakyThrows
    @When("{int} threads concurrently retrieve and cache resource {string} and {string}")
    public void threadsConcurrentlyRetrieveAndCacheResource(int threadCount, String path, String filename) {
        concurrentThreadCount = threadCount;
        concurrentEntries = new ConcurrentLinkedQueue<>();
        concurrentFailures = new ConcurrentLinkedQueue<>();

        // cold start: guarantee a cache miss in every thread to exercise concurrent duplicate loading
        Field cacheField = ResourceUtils.class.getDeclaredField("RESOURCE_CACHE");
        cacheField.setAccessible(true);
        ((Cache<?, ?>) cacheField.get(null)).invalidateAll();

        var startBarrier = new CyclicBarrier(threadCount);

        // try-with-resources waits (close()) for all tasks to finish before the Then step runs
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startBarrier.await();
                        ResourceUtils.findAndCacheResource(path, filename).ifPresent(concurrentEntries::add);
                    } catch (Throwable t) {
                        concurrentFailures.add(t);
                    }
                    return null;
                });
            }
        }
    }

    /**
     * Verifies that all concurrent retrievals succeeded and observed identical resource content, no matter which of the
     * benign duplicate loads won the race.
     */
    @Then("all concurrent retrievals should return the same cached entry")
    public void allConcurrentRetrievalsShouldReturnTheSameCachedEntry() {
        assertThat(concurrentFailures)
                .as("No thread should fail")
                .isEmpty();

        assertThat(concurrentEntries)
                .as("Every thread should receive a cache entry")
                .hasSize(concurrentThreadCount);

        assertThat(concurrentEntries.stream().map(ResourceUtils.CacheEntry::checksum).distinct().count())
                .as("All threads should see identical content")
                .isEqualTo(1L);
    }

}
