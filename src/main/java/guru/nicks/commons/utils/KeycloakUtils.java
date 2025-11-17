package guru.nicks.commons.utils;

import guru.nicks.commons.auth.domain.CustomJwtClaim;

import am.ik.yavi.meta.ConstraintArguments;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Utility class for parsing Keycloak-specific JWT claims.
 */
@UtilityClass
public class KeycloakUtils {

    /**
     * Merges Keycloak realm-level roles (inside {@code realm_access}) and client-level ones (inside
     * {@code resource_access}).
     *
     * @param jwt JWT
     * @return set of roles, sorted for consistency (such as for checksum computation or logging);
     *         {@link Optional#empty()} means no Keycloak-style role container was found in the JWT
     * @throws IllegalArgumentException JWT is {@code null}
     */
    @ConstraintArguments
    public static Optional<SortedSet<String>> tryParseRoles(JwtClaimAccessor jwt) {
        checkNotNull(jwt, _KeycloakUtilsTryParseRolesArgumentsMeta.JWT.name());

        var roles = new TreeSet<String>();
        boolean hasKeycloakRoleContainer = false;

        // {"realm_access": {"roles": ["role1", ...]}} - grab nested 'roles' map, if any
        if (jwt.getClaim(CustomJwtClaim.KEYCLOAK_REALM_ACCESS.getJwtName()) instanceof Map<?, ?> map) {
            hasKeycloakRoleContainer = true;
            extractRolesFromMap(map, roles);
        }

        // {"resource_access": {
        //  "clientId1": {"roles": ["role1", ...]},
        //  "clientId2": {"roles": ["role2", ...]}
        // }} - grab each client's 'roles', if any
        if (jwt.getClaim(CustomJwtClaim.KEYCLOAK_RESOURCE_ACCESS.getJwtName()) instanceof Map<?, ?> map) {
            for (var value : map.values()) {
                if (value instanceof Map<?, ?> clientRolesMap) {
                    hasKeycloakRoleContainer = true;
                    extractRolesFromMap(clientRolesMap, roles);
                }
            }
        }

        return hasKeycloakRoleContainer
                ? Optional.of(roles)
                : Optional.empty();
    }

    /**
     * Extracts role names from a Keycloak JWT claim map and adds them to the target set.
     * <p>
     * This method looks for a 'roles' key in the source map and processes its value if it's a collection. Each role is
     * converted to a string, filtered to exclude blank values, and added to the target set. If the key is not present
     * or its value is not a collection, no roles are added.
     *
     * @param source the map containing JWT claims
     * @param target the set to which extracted role names will be added; must not be {@code null}
     */
    private static void extractRolesFromMap(Map<?, ?> source, Set<String> target) {
        Object rolesValue = source.get(CustomJwtClaim.KEYCLOAK_ROLES.getJwtName());

        if (rolesValue instanceof Collection<?> collection) {
            collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(StringUtils::isNotBlank)
                    .forEach(target::add);
        }
    }

}
