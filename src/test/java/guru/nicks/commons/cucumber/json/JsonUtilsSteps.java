package guru.nicks.commons.cucumber.json;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.json.JsonUtils;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class JsonUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private String firstCanonicalJson;
    private String secondCanonicalJson;

    @When("sensitive JSON fields are masked")
    public void sensitiveJsonFieldsAreMasked() {
        textWorld.setOutput(
                JsonUtils.maskSensitiveJsonFields(
                        // passing bytes internally calls the method accepting a string,
                        // which increases test coverage
                        textWorld.getInput().getBytes(StandardCharsets.UTF_8)));
    }

    @Then("masked JSON should contain {string} for {string}")
    public void maskedJsonShouldContain(String maskedValue, String fieldName) {
        assertThat(textWorld.getOutput().getFirst())
                .contains("\"" + fieldName + "\":" + maskedValue);
    }

    @Then("masked JSON is empty")
    public void maskedJsonIsEmpty() {
        assertThat(textWorld.getOutput()).isEmpty();
    }

    @When("canonical JSON is computed for a top-level set in non-sorted iteration order")
    public void canonicalJsonIsComputedForTopLevelSet() {
        // LinkedHashSet keeps insertion order which differs from the natural one
        var set = new LinkedHashSet<String>();
        set.add("cherry");
        set.add("apple");
        set.add("banana");

        firstCanonicalJson = JsonUtils.sortObjectKeys(set);
    }

    @Then("canonical JSON should be {string}")
    public void canonicalJsonShouldBe(String expectedJson) {
        assertThat(firstCanonicalJson)
                .as("canonical JSON")
                .isEqualTo(expectedJson);
    }

    @When("canonical JSON is computed for two objects containing equal sets in different iteration order")
    public void canonicalJsonIsComputedForTwoObjectsWithEqualSets() {
        // LinkedHashSet iteration order is deterministic and differs between the two objects;
        // a HashSet is added to the mix because its iteration order is hash-based
        var firstSet = new LinkedHashSet<String>();
        firstSet.add("banana");
        firstSet.add("apple");
        firstSet.add("cherry");

        var secondSet = new LinkedHashSet<String>();
        secondSet.add("cherry");
        secondSet.add("banana");
        secondSet.add("apple");

        firstCanonicalJson = JsonUtils.sortObjectKeys(Map.of("tags", firstSet));
        secondCanonicalJson = JsonUtils.sortObjectKeys(Map.of("tags", Set.copyOf(secondSet)));
    }

    @Then("both canonical JSON outputs should be identical")
    public void bothCanonicalJsonOutputsShouldBeIdentical() {
        assertThat(firstCanonicalJson)
                .as("first canonical JSON")
                .isEqualTo(secondCanonicalJson);
    }

    @Then("both canonical JSON outputs should be {string}")
    public void bothCanonicalJsonOutputsShouldBe(String expectedJson) {
        assertThat(firstCanonicalJson)
                .as("first canonical JSON")
                .isEqualTo(expectedJson)
                .isEqualTo(secondCanonicalJson);
    }

}
