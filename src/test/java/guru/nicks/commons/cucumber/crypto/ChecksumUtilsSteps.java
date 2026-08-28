package guru.nicks.commons.cucumber.crypto;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.crypto.ChecksumUtils;
import guru.nicks.commons.utils.json.JsonUtils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class ChecksumUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private TestUser testUser;
    private String sortedJson;
    private String firstChecksum;
    private String secondChecksum;

    @When("JSON checksum is computed for scalar input")
    public void jsonChecksumIsComputedForScalarInput() {
        String input = textWorld.getInput();

        if ("null".equals(input)) {
            input = null;
        }

        String checksum = ChecksumUtils.computeJsonChecksum(input);
        textWorld.setOutput(checksum);
    }

    @Given("test user has name {string} and email {string}")
    public void testUserHasNameAndEmail(String name, String email) {
        testUser = TestUser.builder()
                .name(name)
                .email(email)
                .build();
    }

    @When("JSON checksum is computed for test user")
    public void jsonChecksumIsComputedForTestUser() {
        sortedJson = JsonUtils.sortObjectKeys(testUser);
        String checksum = ChecksumUtils.computeJsonChecksum(testUser);
        textWorld.setOutput(checksum);
    }

    @When("JSON checksums are computed for objects containing equal sets in different iteration order")
    public void jsonChecksumsAreComputedForObjectsWithEqualSets() {
        // LinkedHashSet iteration order is deterministic and differs between the two objects;
        // Set.copyOf produces a hash-ordered set like a HashSet would
        var firstSet = new LinkedHashSet<String>();
        firstSet.add("banana");
        firstSet.add("apple");
        firstSet.add("cherry");

        var secondSet = new LinkedHashSet<String>();
        secondSet.add("cherry");
        secondSet.add("banana");
        secondSet.add("apple");

        var firstObject = Map.of("tags", firstSet);
        var secondObject = Map.of("tags", Set.copyOf(secondSet));

        sortedJson = JsonUtils.sortObjectKeys(firstObject);
        firstChecksum = ChecksumUtils.computeJsonChecksum(firstObject);
        secondChecksum = ChecksumUtils.computeJsonChecksum(secondObject);
    }

    @Then("sorted JSON should be {string}")
    public void sortedJsonShouldBe(String expectedJson) {
        assertThat(sortedJson)
                .as("sorted JSON")
                .isEqualTo(expectedJson);
    }

    @Then("JSON checksum is computed again for scalar input")
    public void jsonChecksumIsComputedAgainForScalarInput() {
        String input = textWorld.getInput();

        if ("null".equals(input)) {
            input = null;
        }

        firstChecksum = textWorld.getOutput().getFirst();
        secondChecksum = ChecksumUtils.computeJsonChecksum(input);
    }

    @Then("both checksums should be identical")
    public void bothChecksumsShouldBeIdentical() {
        assertThat(firstChecksum)
                .as("first checksum")
                .isEqualTo(secondChecksum);
    }

    @Then("checksums should be different")
    public void checksumsShouldBeDifferent() {
        assertThat(firstChecksum)
                .as("first checksum")
                .isNotEqualTo(secondChecksum);
    }

    @Then("output should match Base64 format")
    public void outputShouldMatchBase64Format() {
        String output = textWorld.getOutput().getFirst();
        assertThat(output)
                .as("Base64 format")
                .matches(Pattern.compile("^[A-Za-z0-9+/]+={0,2}$"));
    }

    @Then("output length should be {int}")
    public void outputLengthShouldBe(int expectedLength) {
        String output = textWorld.getOutput().getFirst();
        assertThat(output)
                .as("checksum length")
                .hasSize(expectedLength);
    }

    @Value
    @Builder
    public static class TestUser {

        String name;
        String email;

    }

}
