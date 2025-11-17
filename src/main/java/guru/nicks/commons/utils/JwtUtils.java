package guru.nicks.commons.utils;

import guru.nicks.commons.auth.domain.CustomJwtClaim;
import guru.nicks.commons.auth.domain.JwtProvider;

import am.ik.yavi.meta.ConstraintArguments;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Retrieves JWT components.
 */
@UtilityClass
@Slf4j
public class JwtUtils {

    public static final Locale DEFAULT_USER_LOCALE = Locale.US;

    /**
     * @see #retrieveUserLocale(JwtClaimAccessor)
     */
    private static final Cache<String, Locale> LOCALE_CACHE = Caffeine.newBuilder()
            .maximumSize(50)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    /**
     * Retrieves and post-processes external user IDs according to {@link JwtProvider#getCustomUserIdPrefix()}.
     *
     * @param jwt JWT
     * @return user ID ({@link Pair#getLeft()}, never {@code null}) and JWT provider (may be{@code null})
     * @throws IllegalArgumentException JWT is {@code null}
     * @throws BadJwtException          missing/empty SUB claim
     */
    @ConstraintArguments
    public static Pair<String, JwtProvider> retrieveUserId(JwtClaimAccessor jwt) {
        checkNotNull(jwt, _JwtUtilsRetrieveUserIdArgumentsMeta.JWT.name());

        String userId = jwt.getSubject();
        if (!isValidUsernameOrUserId(userId)) {
            throw new BadJwtException("JWT subject claim is missing or invalid");
        }

        JwtProvider jwtProvider = JwtProvider
                .findByJwtIssuer(jwt.getClaimAsString(JwtClaimNames.ISS))
                .orElse(null);
        if (jwtProvider != null) {
            userId = jwtProvider.getCustomUserIdPrefix() + userId;
        }

        return Pair.of(userId, jwtProvider);
    }

    /**
     * Tries username-holding claims: {@link CustomJwtClaim#PREFERRED_USERNAME}, then {@link CustomJwtClaim#USERNAME}.
     *
     * @param jwt JWT
     * @return optional non-blank username
     * @throws IllegalArgumentException JWT is {@code null}
     */
    @ConstraintArguments
    public static Optional<String> retrieveUsername(JwtClaimAccessor jwt) {
        checkNotNull(jwt, _JwtUtilsRetrieveUsernameArgumentsMeta.JWT.name());

        return Stream.of(CustomJwtClaim.PREFERRED_USERNAME.getJwtName(), CustomJwtClaim.USERNAME.getJwtName())
                .map(jwt::getClaimAsString)
                .filter(StringUtils::isNotBlank)
                .filter(JwtUtils::isValidUsernameOrUserId)
                .findFirst();
    }

    /**
     * Parses array or comma-separated authority list. Does not validate the values, just grabs them as-is. However,
     * doesn't trust authorities passed from foreign JWT providers: returns empty list if {@code jwtProvider} is not
     * {@code null}.
     *
     * @param jwt         JWT
     * @param jwtProvider JWT provider ({@code null} for a local one)
     * @return authorities found, if any
     * @throws IllegalArgumentException JWT is {@code null}
     * @throws BadJwtException          authorities were found in JWT, but it was impossible to parse them
     */
    @ConstraintArguments
    public static Set<String> retrieveAuthorities(JwtClaimAccessor jwt, @Nullable JwtProvider jwtProvider) {
        checkNotNull(jwt, _JwtUtilsRetrieveAuthoritiesArgumentsMeta.JWT.name());

        if (jwtProvider != null) {
            return Collections.emptySet();
        }

        // attempt to read authorities from JWT in this order
        List<Function<JwtClaimAccessor, Optional<?>>> guessers = List.of(
                KeycloakUtils::tryParseRoles,
                jwt1 -> Optional.ofNullable(jwt1.getClaim(CustomJwtClaim.COGNITO_GROUPS.getJwtName())),
                // fallback for 'no roles found in JWT'
                jwt1 -> Optional.empty());

        // stop as soon as one of the guessers returns a non-empty Optional
        Object result = guessers.stream()
                .map(func -> func.apply(jwt))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElse(null);

        return switch (result) {
            case null -> Collections.emptySet();

            // parse comma-separated list
            case String str -> TextUtils.collectUniqueCommaSeparated(str);

            case Collection<?> collection -> collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());

            default -> throw new BadJwtException("JWT authorities must be either string or array");
        };

    }

    /**
     * Parses {@link CustomJwtClaim#LOCALE}, falls back to {@link #DEFAULT_USER_LOCALE}. Caches the result for a while.
     *
     * @param jwt JWT
     * @return locale (never {@code null})
     * @throws IllegalArgumentException JWT is {@code null}
     */
    @ConstraintArguments
    public static Locale retrieveUserLocale(JwtClaimAccessor jwt) {
        checkNotNull(jwt, _JwtUtilsRetrieveUserLocaleArgumentsMeta.JWT.name());

        String claim = jwt.getClaimAsString(CustomJwtClaim.LOCALE.getJwtName());
        if (StringUtils.isBlank(claim)) {
            return DEFAULT_USER_LOCALE;
        }

        // 'get' method may return null as per Caffeine specs, but never does in this particular case
        //noinspection DataFlowIssue
        return LOCALE_CACHE.get(claim, key -> {
            try {
                Locale locale = LocaleUtils.toLocale(key);
                return locale != null
                        ? locale
                        : DEFAULT_USER_LOCALE;
            } catch (RuntimeException e) {
                log.warn("Malformed JWT locale claim '{}' - falling back to '{}'", key, DEFAULT_USER_LOCALE);
                return DEFAULT_USER_LOCALE;
            }
        });
    }

    /**
     * If the JWT has authorized party claim ({@link CustomJwtClaim#AUTHORIZED_PARTY}), returns it, otherwise returns
     * JWT audience ({@link JwtClaimNames#AUD}). The former claim is set for {@code grant_type=client_credentials}, i.e.
     * for programmatic server-to-server clients.
     *
     * @param jwt JWT
     * @return AZP or AUD (never {@code null}), unmodifiable set
     * @throws IllegalArgumentException JWT is {@code null}
     */
    @ConstraintArguments
    public static Set<String> retrieveAzpOrAud(JwtClaimAccessor jwt) {
        checkNotNull(jwt, _JwtUtilsRetrieveAzpOrAudArgumentsMeta.JWT.name());

        // usually an array, but theoretically can be a string
        Collection<?> azp = getClaimAsCollection(jwt, CustomJwtClaim.AUTHORIZED_PARTY.getJwtName());

        Collection<?> values = CollectionUtils.isNotEmpty(azp)
                ? azp
                : getClaimAsCollection(jwt, JwtClaimNames.AUD);

        return values.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves a JWT claim as a collection, handling both single values and collections.
     *
     * @param jwt       JWT
     * @param claimName name of the claim to retrieve
     * @return collection containing the claim value(s), or empty list if the claim is {@code null}
     * @throws IllegalArgumentException JWT is {@code null}
     */
    @ConstraintArguments
    public static Collection<?> getClaimAsCollection(JwtClaimAccessor jwt, String claimName) {
        checkNotNull(jwt, _JwtUtilsGetClaimAsCollectionArgumentsMeta.JWT.name());

        Object claim = jwt.getClaim(claimName);
        if (claim == null) {
            return Collections.emptyList();
        }

        // the top of the hierarchy is Iterable, but its processing is slow
        return (claim instanceof Collection<?> coll)
                ? coll
                : Collections.singletonList(claim.toString());
    }

    /**
     * Creates and signs a JWT, then verifies the signature to ensure consistency. All the arguments must not be null
     *
     * @param header JWT header containing algorithm and key information
     * @param signer JWT signer for creating the signature
     * @param claims JWT claims containing the payload data
     * @param rsaKey RSA key used for signature verification
     * @return signed JWT with verified signature
     * @throws JwtException if signing fails or signature verification fails
     */
    @ConstraintArguments
    public static SignedJWT createSignedJwt(JWSHeader header, JWSSigner signer, JWTClaimsSet claims, RSAKey rsaKey) {
        checkNotNull(header, _JwtUtilsCreateSignedJwtArgumentsMeta.HEADER.name());
        checkNotNull(signer, _JwtUtilsCreateSignedJwtArgumentsMeta.SIGNER.name());
        checkNotNull(claims, _JwtUtilsCreateSignedJwtArgumentsMeta.CLAIMS.name());
        checkNotNull(rsaKey, _JwtUtilsCreateSignedJwtArgumentsMeta.RSAKEY.name());

        var jwt = new SignedJWT(header, claims);

        try {
            jwt.sign(signer);
            // ensure that JWT signature (not expiration time!) can be verified with the given public key
            JWSVerifier signatureVerifier = new RSASSAVerifier(rsaKey);

            if (!jwt.verify(signatureVerifier)) {
                throw new JwtException("Failed to verify newly created JWT");
            }
        } catch (JOSEException e) {
            throw new JwtException("Failed to verify newly created JWT: " + e.getMessage(), e);
        }

        return jwt;
    }

    /**
     * Validates whether a string is a valid username or user ID according to security and formatting constraints. The
     * validation ensures the value is not blank, has a reasonable length, contains no control characters, and has no
     * leading or trailing whitespace.
     *
     * @param value the string to validate as a username or user ID
     * @return {@code true} if the value meets all validation criteria
     */
    private static boolean isValidUsernameOrUserId(String value) {
        return StringUtils.isNotBlank(value)
                && (value.length() <= 255)
                && !value.contains("\n")
                && !value.contains("\r")
                && !value.contains("\0")
                // no leading/trailing whitespace
                && value.trim().equals(value);
    }

}
