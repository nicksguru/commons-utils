package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.ChecksumUtils;
import guru.nicks.commons.utils.JsonUtils;

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

    @When("checksum is computed for scalar input")
    public void checksumIsComputedForScalarInput() {
        String input = textWorld.getInput();

        if ("null".equals(input)) {
            input = null;
        }

        String checksum = ChecksumUtils.computeJsonChecksumBase64(input);
        textWorld.setOutput(checksum);
    }

    @Given("test user has name {string} and email {string}")
    public void testUserHasNameAndEmail(String name, String email) {
        testUser = TestUser.builder()
                .name(name)
                .email(email)
                .build();
    }

    @When("checksum is computed for test user")
    public void checksumIsComputedForTestUser() {
        sortedJson = JsonUtils.sortObjectKeys(testUser);
        String checksum = ChecksumUtils.computeJsonChecksumBase64(testUser);
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
