package guru.nicks.commons.cucumber.crypto;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.crypto.ChecksumUtils;
import guru.nicks.commons.utils.json.JsonUtils;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

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

    @And("sorted JSON should be {string}")
    public void sortedJSONShouldBe(String expectedJson) {
        assertThat(sortedJson)
                .as("sorted JSON")
                .isEqualTo(expectedJson);
    }

    @Value
    @Builder
    public static class TestUser {

        String name;
        String email;

    }

}
