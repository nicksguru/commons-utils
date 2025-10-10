package guru.nicks.cucumber.cache;

import guru.nicks.cache.ChecksumCacheKeyGenerator;
import guru.nicks.cache.domain.CacheConstants;
import guru.nicks.utils.ChecksumUtils;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.Value;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing {@link ChecksumCacheKeyGenerator}.
 */
public class ChecksumCacheKeyGeneratorSteps {

    private final List<Object> parameters = new ArrayList<>();

    @Mock
    private Object targetObject;
    @Mock
    private Method method;
    private AutoCloseable closeableMocks;

    private ChecksumCacheKeyGenerator keyGenerator;
    private Object generatedKey;
    private Object anotherGeneratedKey;
    private ComplexObject complexObject;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        keyGenerator = new ChecksumCacheKeyGenerator();
        parameters.clear();
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("a complex object with nested properties")
    public void aComplexObjectWithNestedProperties() {
        // Create a nested object structure
        NestedObject nestedObject = NestedObject.builder()
                .name("Nested")
                .value(42)
                .build();

        List<String> tags = new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("key1", "value1");
        attributes.put("key2", 123);

        complexObject = ComplexObject.builder()
                .id(1L)
                .name("Complex")
                .active(true)
                .nested(nestedObject)
                .tags(tags)
                .attributes(attributes)
                .build();
    }

    @When("a checksum cache key is generated with string parameters {string} and {string}")
    public void aChecksumCacheKeyIsGeneratedWithStringParameters(String param1, String param2) {
        parameters.add(param1);
        parameters.add(param2);
        generatedKey = keyGenerator.generate(targetObject, method, parameters.toArray());
    }

    @When("a checksum cache key is generated with numeric parameters {int} and {double}")
    public void aChecksumCacheKeyIsGeneratedWithNumericParameters(Integer param1, Double param2) {
        parameters.add(param1);
        parameters.add(param2);
        generatedKey = keyGenerator.generate(targetObject, method, parameters.toArray());
    }

    @When("a checksum cache key is generated with parameters {string} and null")
    public void aChecksumCacheKeyIsGeneratedWithParametersAndNull(String param1) {
        parameters.add(param1);
        parameters.add(null);
        generatedKey = keyGenerator.generate(targetObject, method, parameters.toArray());
    }

    @When("a checksum cache key is generated with parameters null and null")
    public void aChecksumCacheKeyIsGeneratedWithParametersNullAndNull() {
        parameters.add(null);
        parameters.add(null);
        generatedKey = keyGenerator.generate(targetObject, method, parameters.toArray());
    }

    @When("a checksum cache key is generated with the complex object as parameter")
    public void aChecksumCacheKeyIsGeneratedWithTheComplexObjectAsParameter() {
        parameters.add(complexObject);
        generatedKey = keyGenerator.generate(targetObject, method, parameters.toArray());
    }

    @When("a checksum cache key is generated with no parameters")
    public void aChecksumChecksumCacheKeyIsGeneratedWithNoParameters() {
        generatedKey = keyGenerator.generate(targetObject, method);
    }

    @When("a checksum cache key is generated with parameters {string} and {string}")
    public void aChecksumCacheKeyIsGeneratedWithParameters(String param1, String param2) {
        List<Object> newParams = new ArrayList<>();
        newParams.add(param1);
        newParams.add(param2);
        anotherGeneratedKey = keyGenerator.generate(targetObject, method, newParams.toArray());
    }

    @When("a checksum cache key is generated with parameter of type {string}")
    public void aChecksumCacheKeyIsGeneratedWithParameterOfType(String paramType) {
        Object param = switch (paramType) {
            case "String" -> "test string";
            case "Integer" -> 42;
            case "Double" -> 3.14159;
            case "Boolean" -> true;
            case "List" -> List.of("item1", "item2", "item3");
            case "Map" -> Map.of("key1", "value1", "key2", "value2");
            case "CustomObject" -> ComplexObject.builder()
                    .id(999L)
                    .name("Test Object")
                    .active(true)
                    .build();
            default -> null;
        };

        parameters.add(param);
        generatedKey = keyGenerator.generate(targetObject, method, parameters.toArray());
    }

    @Then("the generated key should contain the checksum of each parameter")
    public void theGeneratedKeyShouldContainTheChecksumOfEachParameter() {
        assertThat(generatedKey)
                .as("generatedKey")
                .isNotNull()
                .isInstanceOf(String.class);

        String key = (String) generatedKey;

        // split the key by the delimiter
        String[] checksums = key.split(CacheConstants.TOPIC_DELIMITER);

        // verify we have the right number of checksums
        assertThat(checksums)
                .as("checksums")
                .hasSize(parameters.size());

        // verify each checksum
        for (int i = 0; i < parameters.size(); i++) {
            String expectedChecksum = ChecksumUtils.computeJsonChecksumBase64(parameters.get(i));
            assertThat(checksums[i])
                    .as("checksums[%d]", i)
                    .isEqualTo(expectedChecksum);
        }
    }

    @Then("the checksums should be joined with the topic delimiter")
    public void theChecksumsShouldBeJoinedWithTheTopicDelimiter() {
        assertThat(generatedKey)
                .as("generatedKey")
                .isNotNull()
                .isInstanceOf(String.class);

        String key = (String) generatedKey;

        // generate expected checksums
        List<String> expectedChecksums = parameters.stream()
                .map(ChecksumUtils::computeJsonChecksumBase64)
                .toList();

        // join them with the delimiter
        String expected = String.join(CacheConstants.TOPIC_DELIMITER, expectedChecksums);

        // Verify the key matches the expected format
        assertThat(key)
                .as("key")
                .isEqualTo(expected);
    }

    @Then("the generated key should contain the JSON checksum of the object")
    public void theGeneratedKeyShouldContainTheJSONChecksumOfTheObject() {
        assertThat(generatedKey)
                .as("generatedKey")
                .isNotNull()
                .isInstanceOf(String.class);

        String key = (String) generatedKey;
        String expectedChecksum = ChecksumUtils.computeJsonChecksumBase64(complexObject);

        assertThat(key)
                .as("key")
                .isEqualTo(expectedChecksum);
    }

    @Then("the generated key should be an empty string")
    public void theGeneratedKeyShouldBeAnEmptyString() {
        assertThat(generatedKey)
                .as("generatedKey")
                .isEqualTo("");
    }

    @Then("the two generated keys should be different")
    public void theTwoGeneratedKeysShouldBeDifferent() {
        assertThat(generatedKey)
                .as("generatedKey")
                .isNotEqualTo(anotherGeneratedKey);
    }

    @Then("the generated key should equal {string}")
    public void theGeneratedKeyShouldEqual(String expectedChecksum) {
        assertThat(generatedKey)
                .as("generatedKey")
                .isNotNull()
                .isInstanceOf(String.class);

        String key = (String) generatedKey;

        assertThat(key)
                .as("key")
                .isEqualTo(expectedChecksum);
    }

    /**
     * Complex object with nested properties for testing JSON serialization.
     */
    @Value
    @Builder
    private static class ComplexObject {

        Long id;
        String name;
        boolean active;
        NestedObject nested;
        List<String> tags;
        Map<String, Object> attributes;

    }

    /**
     * Nested object for testing complex object serialization.
     */
    @Value
    @Builder
    private static class NestedObject {

        String name;
        int value;

    }
}
