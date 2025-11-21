package guru.nicks.commons.cucumber.auth;

import guru.nicks.commons.auth.domain.CustomJwtClaim;
import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.auth.KeycloakUtils;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

/**
 * Step definitions for testing {@link KeycloakUtils}.
 */
@RequiredArgsConstructor
public class KeycloakUtilsSteps {

    // DI
    private final TextWorld textWorld;

    @Mock
    private JwtClaimAccessor jwtClaimAccessorMock;
    private AutoCloseable closeableMocks;

    private Optional<SortedSet<String>> actualRoles;
    private JwtClaimAccessor jwt;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @DataTableType
    public RolesInput createRolesInput(Map<String, String> row) {
        return RolesInput.builder()
                .realmRoles(row.get("realmRoles"))
                .clientOneRoles(row.get("clientOneRoles"))
                .clientTwoRoles(row.get("clientTwoRoles"))
                .build();
    }

    @Given("a JWT is provided with the following roles:")
    public void aJwtIsProvidedWithTheFollowingRoles(RolesInput rolesInput) {
        jwt = jwtClaimAccessorMock;
        var realmAccess = new TreeMap<String, Object>();

        if (isNotBlank(rolesInput.realmRoles)) {
            var roles = parseRoles(rolesInput.realmRoles);
            realmAccess.put(CustomJwtClaim.KEYCLOAK_ROLES.getJwtName(), roles);

            when(jwtClaimAccessorMock.getClaim(CustomJwtClaim.KEYCLOAK_REALM_ACCESS.getJwtName()))
                    .thenReturn(realmAccess);
        }

        var resourceAccess = new TreeMap<String, Object>();
        if (isNotBlank(rolesInput.clientOneRoles)) {
            var clientRoles = Map.of(CustomJwtClaim.KEYCLOAK_ROLES.getJwtName(), parseRoles(rolesInput.clientOneRoles));
            resourceAccess.put("client-one", clientRoles);
        }

        if (isNotBlank(rolesInput.clientTwoRoles)) {
            var clientRoles = Map.of(CustomJwtClaim.KEYCLOAK_ROLES.getJwtName(), parseRoles(rolesInput.clientTwoRoles));
            resourceAccess.put("client-two", clientRoles);
        }

        if (!resourceAccess.isEmpty()) {
            when(jwtClaimAccessorMock.getClaim(CustomJwtClaim.KEYCLOAK_RESOURCE_ACCESS.getJwtName()))
                    .thenReturn(resourceAccess);
        }
    }

    @Given("a null JWT is provided")
    public void aNullJwtIsProvided() {
        jwt = null;
    }

    @When("roles are parsed from the JWT")
    public void rolesAreParsedFromTheJwt() {
        var throwable = catchThrowable(() -> actualRoles = KeycloakUtils.tryParseRoles(jwt));
        textWorld.setLastException(throwable);
    }

    @Then("the parsed roles should be {string}")
    public void theParsedRolesShouldBe(String expectedRolesString) {
        if (actualRoles.isPresent()) {
            var expectedRoles = isNotBlank(expectedRolesString)
                    ? new TreeSet<>(Arrays.asList(expectedRolesString.split(",")))
                    : new TreeSet<String>();
            assertThat(actualRoles.get())
                    .as("actualRoles")
                    .isEqualTo(expectedRoles);
        }
    }

    @Then("a set of roles is expected to be present: {booleanValue}")
    public void aSetOfRolesIsExpectedToBePresent(boolean expected) {
        assertThat(actualRoles.isPresent())
                .as("roles.isPresent()")
                .isEqualTo(expected);
    }

    private List<String> parseRoles(String roles) {
        return Arrays.stream(roles.split(","))
                .map(String::strip)
                .collect(Collectors.toList());
    }

    /**
     * Data table input for roles.
     */
    @Value
    @Builder
    public static class RolesInput {
        String realmRoles;
        String clientOneRoles;
        String clientTwoRoles;
    }

}
