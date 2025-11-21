package guru.nicks.commons.cucumber.auth;

import guru.nicks.commons.auth.domain.BasicAuthCredentials;
import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.auth.AuthUtils;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for testing {@link AuthUtils} functionality.
 */
@RequiredArgsConstructor
public class AuthUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private final List<String> checksums = new ArrayList<>();
    private final List<TokenData> tokens = new ArrayList<>();
    private final List<String> tokenChecksums = new ArrayList<>();

    private String accessToken;
    private String checksum;

    private String basicAuthHeader;
    private BasicAuthCredentials basicAuthCredentials;

    @DataTableType
    public TokenData createTokenData(Map<String, String> entry) {
        return TokenData.builder()
                .token(entry.get("token"))
                .build();
    }

    @Given("an access token {string}")
    public void anAccessToken(String token) {
        accessToken = token;
    }

    @Given("the following access tokens:")
    public void theFollowingAccessTokens(List<TokenData> tokenDataList) {
        tokens.clear();
        tokens.addAll(tokenDataList);
    }

    @Given("a basic auth header {string}")
    public void aBasicAuthHeader(String header) {
        basicAuthHeader = header;
    }

    @When("the access token checksum is calculated")
    public void theAccessTokenChecksumIsCalculated() {
        textWorld.setLastException(catchThrowable(() ->
                checksum = AuthUtils.calculateAccessTokenChecksum(accessToken)));
    }

    @When("the access token checksum is calculated multiple times")
    public void theAccessTokenChecksumIsCalculatedMultipleTimes() {
        checksums.clear();

        for (int i = 0; i < 5; i++) {
            checksums.add(AuthUtils.calculateAccessTokenChecksum(accessToken));
        }
    }

    @When("checksums are calculated for all tokens")
    public void checksumsAreCalculatedForAllTokens() {
        tokenChecksums.clear();

        for (TokenData tokenData : tokens) {
            tokenChecksums.add(AuthUtils.calculateAccessTokenChecksum(tokenData.getToken()));
        }
    }

    @When("the access token checksum is calculated for a null token")
    public void theAccessTokenChecksumIsCalculatedForANullToken() {
        textWorld.setLastException(catchThrowable(() ->
                checksum = AuthUtils.calculateAccessTokenChecksum(null)));
    }

    @When("the basic auth header is parsed")
    public void theBasicAuthHeaderIsParsed() {
        textWorld.setLastException(catchThrowable(() ->
                basicAuthCredentials = AuthUtils.parseBasicAuthHeader(basicAuthHeader)));
    }

    @When("the basic auth header is parsed with a null header")
    public void theBasicAuthHeaderIsParsedWithANullHeader() {
        textWorld.setLastException(catchThrowable(() ->
                basicAuthCredentials = AuthUtils.parseBasicAuthHeader(null)));
    }

    @Then("the checksum should contain {string}")
    public void theChecksumShouldContain(String expectedSubstring) {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();
        assertThat(checksum)
                .as("checksum")
                .contains(expectedSubstring);
    }

    @Then("the checksum should not contain special characters")
    public void theChecksumShouldNotContainSpecialCharacters() {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();

        // Check for common special characters that could cause issues in JMX bean search strings
        assertThat(checksum)
                .as("checksum")
                .doesNotContain("=", ":", "/", "\\", "?", "*", "\"", "<", ">", "|");
    }

    @Then("all checksums should be identical")
    public void allChecksumsShouldBeIdentical() {
        assertThat(checksums)
                .as("checksums")
                .isNotEmpty();

        String firstChecksum = checksums.getFirst();

        for (int i = 1; i < checksums.size(); i++) {
            assertThat(checksums.get(i))
                    .as("checksums[" + i + "]")
                    .isEqualTo(firstChecksum);
        }
    }

    @Then("each checksum should be unique")
    public void eachChecksumShouldBeUnique() {
        assertThat(tokenChecksums)
                .as("tokenChecksums")
                .isNotEmpty();

        Set<String> uniqueChecksums = new HashSet<>(tokenChecksums);
        assertThat(uniqueChecksums)
                .as("uniqueChecksums.size()")
                .hasSameSizeAs(tokenChecksums);
    }

    @Then("the username should be {string}")
    public void theUsernameShouldBe(String expectedUsername) {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();

        assertThat(basicAuthCredentials.getUsername())
                .as("basicAuthCredentials.getLeft()")
                .isEqualTo(expectedUsername);
    }

    @Then("the password should be {string}")
    public void thePasswordShouldBe(String expectedPassword) {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();

        assertThat(basicAuthCredentials.getPassword())
                .as("basicAuthCredentials.getRight()")
                .isEqualTo(expectedPassword);
    }

    @And("the checksum should equal {string}")
    public void theChecksumShouldEqual(String expectedChecksum) {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNull();

        assertThat(checksum)
                .as("checksum")
                .isEqualTo(expectedChecksum);
    }

    /**
     * Data class for token data.
     */
    @Value
    @Builder
    public static class TokenData {

        String token;

    }

}
