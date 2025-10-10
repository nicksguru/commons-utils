package guru.nicks.cucumber.cache;

import guru.nicks.cache.ToStringJoiningCacheKeyGenerator;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing {@link ToStringJoiningCacheKeyGenerator}.
 */
public class ToStringJoiningCacheKeyGeneratorSteps {

    private final List<Object> parameters = new ArrayList<>();
    @Mock
    private Object targetObject;
    @Mock
    private Method method;
    private AutoCloseable closeableMocks;

    private ToStringJoiningCacheKeyGenerator keyGenerator;
    private Object generatedKey;
    private CustomObject customObject;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        keyGenerator = new ToStringJoiningCacheKeyGenerator();
        parameters.clear();
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @Given("a custom object with toString returning {string}")
    public void aCustomObjectWithToStringReturning(String toStringValue) {
        customObject = CustomObject.builder()
                .toStringValue(toStringValue)
                .build();
    }

    @When("a cache key is generated with string parameters {string} and {string}")
    public void aCacheKeyIsGeneratedWithStringParameters(String param1, String param2) {
        parameters.add(param1);
        parameters.add(param2);
        generatedKey = keyGenerator.generate(targetObject, method, parameters.toArray());
    }

    @When("a cache key is generated with numeric parameters {int} and {double}")
    public void aCacheKeyIsGeneratedWithNumericParameters(Integer param1, Double param2) {
        parameters.add(param1);
        parameters.add(param2);
        generatedKey = keyGenerator.generate(targetObject, method, parameters.toArray());
    }

    @When("a cache key is generated with parameters {string} and null")
    public void aCacheKeyIsGeneratedWithParametersAndNull(String param1) {
        parameters.add(param1);
        parameters.add(null);
        generatedKey = keyGenerator.generate(targetObject, method, parameters.toArray());
    }

    @When("a cache key is generated with parameters null and null")
    public void aCacheKeyIsGeneratedWithParametersNullAndNull() {
        parameters.add(null);
        parameters.add(null);
        generatedKey = keyGenerator.generate(targetObject, method, parameters.toArray());
    }

    @When("a cache key is generated with parameters {string} and {string}")
    public void aCacheKeyIsGeneratedWithParameters(String param1, String param2) {
        parameters.add(param1);
        parameters.add(param2);
        generatedKey = keyGenerator.generate(targetObject, method, parameters.toArray());
    }

    @When("a cache key is generated with parameters {string} and the custom object")
    public void aCacheKeyIsGeneratedWithParametersAndTheCustomObject(String param1) {
        parameters.add(param1);
        parameters.add(customObject);
        generatedKey = keyGenerator.generate(targetObject, method, parameters.toArray());
    }

    @When("a cache key is generated with no parameters")
    public void aCacheKeyIsGeneratedWithNoParameters() {
        generatedKey = keyGenerator.generate(targetObject, method);
    }

    @Then("the generated key should be {string}")
    public void theGeneratedKeyShouldBe(String expectedKey) {
        assertThat(generatedKey)
                .as("generatedKey")
                .isEqualTo(expectedKey);
    }

    /**
     * Custom object with a predefined toString result.
     */
    @Value
    @Builder
    private static class CustomObject {

        String toStringValue;

        @Override
        public String toString() {
            return toStringValue;
        }

    }
}
