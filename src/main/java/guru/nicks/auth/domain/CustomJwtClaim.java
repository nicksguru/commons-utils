package guru.nicks.auth.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Non-standard JWT claim names.
 */
@RequiredArgsConstructor
@Getter
public enum CustomJwtClaim {

    /**
     * Passed by Keycloak for {@code client_credentials} grant type.
     */
    AUTHORIZED_PARTY("azp"),

    EMAIL("email"),

    /**
     * Boolean claim.
     */
    EMAIL_VERIFIED("email_verified"),

    PREFERRED_USERNAME("preferred_username"),
    USERNAME("username"),

    /**
     * In Keycloak, this is an object listing realm-level roles assigned to user:
     * {@code {"realm_access": {"roles": ["role1", "role2"]}}}.
     *
     * @see #KEYCLOAK_ROLES
     */
    KEYCLOAK_REALM_ACCESS("realm_access"),

    /**
     * In Keycloak, this is an object listing client-level roles assigned to user:
     * {@code {"resource_access": {"clientId1": {"roles": ["role1", ...]}, "clientId2": {"roles": ["role2", ...]}}}}.
     *
     * @see #KEYCLOAK_ROLES
     */
    KEYCLOAK_RESOURCE_ACCESS("resource_access"),

    /**
     * Nested key in {@link #KEYCLOAK_REALM_ACCESS} and {@link #KEYCLOAK_RESOURCE_ACCESS}- JSON array of role names.
     */
    KEYCLOAK_ROLES("roles"),

    /**
     * Groups (can be treated as a role name) passed by AWS Cognito.
     */
    COGNITO_GROUPS("cognito:groups"),

    /**
     * Open ID Connect <a href="https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims">standard
     * claims</a>.
     */
    GIVEN_NAME("given_name"),
    FAMILY_NAME("family_name"),
    NAME("name"),
    PICTURE("picture"),
    LOCALE("locale");

    private final String jwtName;

}
