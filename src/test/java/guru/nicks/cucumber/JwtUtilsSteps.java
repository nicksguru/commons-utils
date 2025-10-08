package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.domain.CustomJwtClaim;
import guru.nicks.user.domain.JwtProvider;
import guru.nicks.utils.JwtUtils;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class JwtUtilsSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private JwtClaimAccessor jwtClaimAccessor;
    private AutoCloseable closeableMocks;

    private Pair<String, JwtProvider> userIdResult;
    private String usernameResult;
    private Set<String> authoritiesResult;
    private Locale localeResult;
    private Set<String> azpOrAudResult;

    private JwtProvider jwtProvider;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @DataTableType
    public ClaimEntry createClaimEntry(Map<String, String> entry) {
        return ClaimEntry.builder()
                .claim(entry.get("claim"))
                .value(entry.get("value"))
                .build();
    }

    @DataTableType
    public AuthorityConfig createAuthorityConfig(Map<String, String> entry) {
        return AuthorityConfig.builder()
                .type(entry.get("type"))
                .value(entry.get("value"))
                .build();
    }

    @Given("a JWT with subject {string} and issuer {string}")
    public void aJwtWithSubjectAndIssuer(String subject, String issuer) {
        when(jwtClaimAccessor.getSubject())
                .thenReturn(subject);
        when(jwtClaimAccessor.getClaimAsString(JwtClaimNames.ISS))
                .thenReturn(issuer);
    }

    @Given("a JWT with the following claims:")
    public void aJwtWithTheFollowingClaims(List<ClaimEntry> claims) {
        for (ClaimEntry claim : claims) {
            if ((claim.getValue() != null)
                    && claim.getValue().startsWith("[")
                    && claim.getValue().endsWith("]")) {
                // handle array values
                String[] values = claim
                        .getValue()
                        .substring(1, claim.getValue().length() - 1)
                        .split(",");

                List<String> valueList = Arrays.stream(values)
                        .map(v -> v.replace("\"", "").strip())
                        .toList();

                when(jwtClaimAccessor.getClaim(claim.getClaim()))
                        .thenReturn(valueList);
            } else {
                when(jwtClaimAccessor.getClaimAsString(claim.getClaim()))
                        .thenReturn(claim.getValue());
                when(jwtClaimAccessor.getClaim(claim.getClaim()))
                        .thenReturn(claim.getValue());
            }
        }
    }

    @Given("a JWT with the following authorities configuration:")
    public void aJwtWithTheFollowingAuthoritiesConfiguration(List<AuthorityConfig> configs) {
        for (AuthorityConfig config : configs) {
            switch (config.getType()) {
                case "keycloak" -> setupKeycloakRoles(config.getValue());
                case "cognito_groups" -> {
                    if (config.getValue().startsWith("[") && config.getValue().endsWith("]")) {
                        String[] values = config
                                .getValue()
                                .substring(1, config.getValue().length() - 1)
                                .split(",");

                        List<String> valueList = Arrays.stream(values)
                                .map(v -> v.replace("\"", "").strip())
                                .toList();

                        when(jwtClaimAccessor.getClaim(CustomJwtClaim.COGNITO_GROUPS.getJwtName()))
                                .thenReturn(valueList);
                    }
                }

                case "invalid" -> when(jwtClaimAccessor.getClaim("authorities"))
                        .thenReturn(Integer.valueOf(config.getValue()));
            }
        }
    }

    @Given("JWT provider is {string}")
    public void jwtProviderIs(String originName) {
        if (StringUtils.isNotBlank(originName)) {
            jwtProvider = JwtProvider.valueOf(originName);
        } else {
            jwtProvider = null;
        }
    }

    @Given("a JWT with locale claim {string}")
    public void aJwtWithLocaleClaim(String locale) {
        when(jwtClaimAccessor.getClaimAsString(CustomJwtClaim.LOCALE.getJwtName()))
                .thenReturn(locale);
    }

    @When("the user ID is retrieved from the JWT")
    public void theUserIdIsRetrievedFromTheJwt() {
        Throwable thrown = catchThrowable(() ->
                userIdResult = JwtUtils.retrieveUserId(jwtClaimAccessor));

        textWorld.setLastException(thrown);
    }

    @When("the username is retrieved with default value {string}")
    public void theUsernameIsRetrievedWithDefaultValue(String defaultValue) {
        Throwable thrown = catchThrowable(() ->
                usernameResult = JwtUtils.retrieveUsername(jwtClaimAccessor).orElse(defaultValue));

        textWorld.setLastException(thrown);
    }

    @When("the authorities are retrieved from the JWT")
    public void theAuthoritiesAreRetrievedFromTheJwt() {
        textWorld.setLastException(catchThrowable(() ->
                authoritiesResult = JwtUtils.retrieveAuthorities(jwtClaimAccessor, jwtProvider)));
    }

    @When("the user locale is retrieved from the JWT")
    public void theUserLocaleIsRetrievedFromTheJwt() {
        Throwable thrown = catchThrowable(() ->
                localeResult = JwtUtils.retrieveUserLocale(jwtClaimAccessor));

        textWorld.setLastException(thrown);
    }

    @When("the AZP or AUD is retrieved from the JWT")
    public void theAzpOrAudIsRetrievedFromTheJwt() {
        Throwable thrown = catchThrowable(() ->
                azpOrAudResult = JwtUtils.retrieveAzpOrAud(jwtClaimAccessor));

        textWorld.setLastException(thrown);
    }

    @Then("the user ID should be {string}")
    public void theUserIdShouldBe(String expectedUserId) {
        assertThat(userIdResult.getLeft())
                .as("userId")
                .isEqualTo(expectedUserId);
    }

    @Then("the JWT provider should be {string}")
    public void theJwtProviderShouldBe(String expectedJwtProvider) {
        if (StringUtils.isBlank(expectedJwtProvider)) {
            assertThat(userIdResult.getRight())
                    .as("jwtProvider")
                    .isNull();
        } else {
            assertThat(userIdResult.getRight())
                    .as("jwtProvider")
                    .isEqualTo(JwtProvider.valueOf(expectedJwtProvider));
        }
    }

    @Then("the retrieved username should be {string}")
    public void theRetrievedUsernameShouldBe(String expectedUsername) {
        assertThat(usernameResult)
                .as("username")
                .isEqualTo(expectedUsername);
    }

    @Then("the authorities should contain {string}")
    public void theAuthoritiesShouldContain(String expectedAuthorities) {
        if (StringUtils.isBlank(expectedAuthorities)) {
            assertThat(authoritiesResult)
                    .as("authorities")
                    .isEmpty();
        } else {
            String[] expected = expectedAuthorities.split(",");
            assertThat(authoritiesResult)
                    .as("authorities")
                    .containsExactlyInAnyOrder(expected);
        }
    }

    @Then("the retrieved locale should be {string}")
    public void theRetrievedLocaleShouldBe(String expectedLocale) {
        assertThat(localeResult)
                .as("locale")
                .hasToString(expectedLocale);
    }

    @Then("the retrieved values should contain {string}")
    public void theRetrievedValuesShouldContain(String expectedValues) {
        if (StringUtils.isBlank(expectedValues)) {
            assertThat(azpOrAudResult)
                    .as("azpOrAud")
                    .isEmpty();
        } else {
            String[] expected = expectedValues.split(",");

            assertThat(azpOrAudResult)
                    .as("azpOrAud")
                    .containsExactlyInAnyOrder(expected);
        }
    }

    private void setupKeycloakRoles(String jsonConfig) {
        if (jsonConfig.contains("realm_access")) {
            var realmAccessMap = Map.of("roles",
                    Arrays.stream(
                                    StringUtils.substringBetween(jsonConfig, "\"realm_access\":{\"roles\":[", "]")
                                            .split(","))
                            .map(s -> s.replace("\"", ""))
                            .toList());

            when(jwtClaimAccessor.getClaim(CustomJwtClaim.KEYCLOAK_REALM_ACCESS.getJwtName()))
                    .thenReturn(realmAccessMap);
        }

        if (jsonConfig.contains("resource_access")) {
            String resourceAccessStr = StringUtils.substringBetween(jsonConfig, "\"resource_access\":{", "}");
            String clientId = StringUtils.substringBefore(resourceAccessStr, ":{");
            clientId = clientId.replace("\"", "");

            String rolesStr = StringUtils.substringBetween(resourceAccessStr, "\"roles\":[", "]");
            List<String> roles = Arrays.stream(rolesStr.split(","))
                    .map(s -> s.replace("\"", ""))
                    .toList();

            var clientRolesMap = Map.of("roles", roles);
            var resourceAccessMap = Map.of(clientId, clientRolesMap);

            when(jwtClaimAccessor.getClaim(CustomJwtClaim.KEYCLOAK_RESOURCE_ACCESS.getJwtName()))
                    .thenReturn(resourceAccessMap);
        }
    }

    @Value
    @Builder
    public static class ClaimEntry {

        String claim;
        String value;

    }

    @Value
    @Builder
    public static class AuthorityConfig {

        String type;
        String value;

    }
}
