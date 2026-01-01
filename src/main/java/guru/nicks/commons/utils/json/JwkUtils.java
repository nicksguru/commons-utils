package guru.nicks.commons.utils.json;

import guru.nicks.commons.auth.domain.JwkInfo;
import guru.nicks.commons.utils.TransformUtils;
import guru.nicks.commons.utils.crypto.PemUtils;
import guru.nicks.commons.utils.text.TimeUtils;

import am.ik.yavi.meta.ConstraintArguments;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.CacheControl;
import okhttp3.Headers;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNullNested;

/**
 * JWKS represent public keys, see <a href="https://datatracker.ietf.org/doc/html/rfc7517">IETF</a>.
 */
@UtilityClass
@Slf4j
public class JwkUtils {

    /**
     * Finds the smallest {@link JwkInfo#expirationDate()} - it can even be in the past.
     *
     * @param jwkInfos JWKs
     * @return optional smallest expiration date
     */
    @ConstraintArguments
    public static Optional<Instant> findSmallestExpirationDate(Collection<? extends JwkInfo> jwkInfos) {
        checkNotNull(jwkInfos, _JwkUtilsFindSmallestExpirationDateArgumentsMeta.JWKINFOS.name());

        return jwkInfos.stream()
                .map(JwkInfo::expirationDate)
                .filter(Objects::nonNull)
                .min(Instant::compareTo);
    }

    /**
     * Extracts public RSA keys from their JWK metadata.
     *
     * @param jwkInfo JWKS
     * @return RSA public keys
     * @throws JwtException if the JWK does not contain a valid RSA key
     */
    @ConstraintArguments
    public static List<RSAPublicKey> extractPublicKeys(JwkInfo jwkInfo) {
        checkNotNullNested(jwkInfo, _JwkUtilsExtractPublicKeysArgumentsMeta.JWKINFO.name(), JwkInfo::keys, "keys");
        return TransformUtils.toList(jwkInfo.keys().toPublicJWKSet().getKeys(), JwkUtils::convertToRsaPublicKey);
    }

    /**
     * Creates JWT decoder which verifies JWTs with the given public key.
     *
     * @param publicKey public key
     * @return JWT decoder
     */
    @ConstraintArguments
    public static JwtDecoder createJwtDecoder(RSAPublicKey publicKey) {
        checkNotNull(publicKey, _JwkUtilsCreateJwtDecoderArgumentsMeta.PUBLICKEY.name());

        // often public keys use 'RSA' instead of 'RSA256'
        SignatureAlgorithm algorithm = "RSA".equals(publicKey.getAlgorithm())
                ? SignatureAlgorithm.RS256
                : SignatureAlgorithm.valueOf(publicKey.getAlgorithm());

        return NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(algorithm)
                .build();
    }

    /**
     * Fetches JWKS from the given URL. Honors {@code Cache-Control} header set, for example, by Google - assigns
     * {@link JwkInfo#expirationDate()}.
     *
     * @param authProviderId ID to assign to the JWKS ({@link JwkInfo#authProviderId()})
     * @param url            URL to fetch JWKS from
     * @param restClient     REST client
     * @return JWKS
     */
    @ConstraintArguments
    public static JwkInfo fetchJwkSet(String authProviderId, String url, RestOperations restClient) {
        checkNotBlank(authProviderId, _JwkUtilsFetchJwkSetArgumentsMeta.AUTHPROVIDERID.name());
        checkNotBlank(url, _JwkUtilsFetchJwkSetArgumentsMeta.URL.name());
        checkNotNull(restClient, _JwkUtilsFetchJwkSetArgumentsMeta.RESTCLIENT.name());

        log.debug("Fetching JWKS for auth provider '{}' from '{}'", authProviderId, url);

        HttpEntity<String> response = fetchJwkSetResponse(url, restClient);
        JWKSet jwkSet = parseAndValidateJwkSet(response, url);

        JwkInfo jwkInfo = JwkInfo.builder()
                .authProviderId(authProviderId)
                .keys(jwkSet)
                .build();

        return applyExpirationFromCacheControl(jwkInfo, response, authProviderId, url);
    }

    /**
     * Creates JWK (JSON Web Key) out of key pair.
     *
     * @param pem key pair in PEM format
     * @return JWK (consider using {@link JWK#toPublicJWK()} to hide the private key if it's not needed)
     */
    public static JWK parsePemToJwk(String pem) {
        String fixedPem = PemUtils.fixPem(pem);

        try {
            return JWK.parseFromPEMEncodedObjects(fixedPem);
        } catch (JOSEException e) {
            throw new JwtException("Failed to parse JWK: " + e.getMessage(), e);
        }
    }

    /**
     * Safely converts a JWK to RSA public key with proper error handling.
     *
     * @param jwk the JWK to convert
     * @return RSA public key
     * @throws JwtException if conversion fails
     */
    private static RSAPublicKey convertToRsaPublicKey(JWK jwk) {
        try {
            // internally does '(RSAKey) this', hence ClassCastException
            return jwk.toRSAKey().toRSAPublicKey();
        } catch (ClassCastException | JOSEException e) {
            throw new JwtException("Failed to get RSA public key from key ID '"
                    + jwk.getKeyID()
                    + "': "
                    + e.getMessage(), e);
        }
    }

    /**
     * Fetches the JWK set response from the specified URL using HTTP GET request. Wraps any REST client exceptions into
     * JWT-specific exceptions for consistent error handling.
     *
     * @param url        the URL endpoint from which to fetch the JWK set
     * @param restClient the REST operations client used to perform the HTTP request
     * @return the HTTP response entity containing the JWK set as a string body
     * @throws JwtException if the HTTP request fails or encounters any REST client errors
     */
    private static HttpEntity<String> fetchJwkSetResponse(String url, RestOperations restClient) {
        try {
            return restClient.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, String.class);
        } catch (RestClientException e) {
            throw new JwtException("Failed to fetch JWK set from '" + url + "': " + e.getMessage(), e);
        }
    }

    /**
     * Parses and validates a JWK set from an HTTP response body. Ensures the response contains valid JSON that can be
     * parsed into a JWK set and that the resulting set contains at least one key.
     *
     * @param response the HTTP response entity containing the JWK set JSON in the response body
     * @param url      the URL from which the JWK set was fetched, used for error reporting and logging
     * @return a validated {@link JWKSet} containing at least one key
     * @throws JwtException if the response body is blank / parsing fails / the JWK set contains no keys
     */
    private static JWKSet parseAndValidateJwkSet(HttpEntity<String> response, String url) {
        String responseBody = response.getBody();
        if (StringUtils.isBlank(responseBody)) {
            throw new JwtException("Received blank response body when fetching JWK set from '" + url + "'");
        }

        try {
            JWKSet jwkSet = JWKSet.parse(responseBody);
            return checkNotNull(jwkSet, "jwkSet");
        } catch (Exception e) {
            throw new JwtException("Failed to parse JWK set fetched from '" + url + "': " + e.getMessage(), e);
        }
    }

    /**
     * Applies expiration date to JWK info based on the {@code Cache-Control} header from the HTTP response by parsing
     * the {@code max-age} directive. If no {@code Cache-Control} header is present or {@code max-age} is negative, the
     * JWK info is returned unchanged (indicating keys never expire). Logs the fetching result with appropriate
     * expiration information.
     *
     * @param jwkInfo        the JWK info object to potentially update with expiration date
     * @param response       the HTTP response entity containing headers from the JWK set fetch request
     * @param authProviderId the authentication provider identifier, used for logging purposes
     * @param url            the URL from which the JWK set was fetched, used for logging and error reporting
     * @return the original JWK info if no expiration should be applied, or a new JWK info instance with the expiration
     *         date set based on the Cache-Control max-age directive
     */
    private static JwkInfo applyExpirationFromCacheControl(JwkInfo jwkInfo, HttpEntity<String> response,
            String authProviderId, String url) {
        HttpHeaders responseHeaders = response.getHeaders();
        String cacheControlHeader = responseHeaders.getCacheControl();

        if (StringUtils.isBlank(cacheControlHeader)) {
            logJwkSetFetched(jwkInfo, authProviderId, url, "they never expire (no Cache-Control header)");
            return jwkInfo;
        }

        try {
            var headers = new Headers.Builder().add(HttpHeaders.CACHE_CONTROL, cacheControlHeader).build();
            int maxAgeSec = CacheControl.parse(headers).maxAgeSeconds();

            if (maxAgeSec < 0) {
                logJwkSetFetched(jwkInfo, authProviderId, url, "they never expire (max-age < 0)");
                return jwkInfo;
            }

            var now = Instant.now();
            JwkInfo updatedJwkInfo = jwkInfo.toBuilder()
                    // if maxAgeSec is too large, DateTimeException or ArithmeticException will be thrown
                    .expirationDate(now.plusSeconds(maxAgeSec))
                    .build();

            String expirationInfo = String.format("at least one of them expires in %s (at %s)",
                    TimeUtils.humanFormatDuration(Duration.between(now, updatedJwkInfo.expirationDate())),
                    updatedJwkInfo.expirationDate());
            logJwkSetFetched(updatedJwkInfo, authProviderId, url, expirationInfo);

            return updatedJwkInfo;
        } catch (RuntimeException e) {
            log.warn("Failed to parse cache control header '{}' for JWK set from '{}': {}",
                    cacheControlHeader, url, e.getMessage());
            return jwkInfo;
        }
    }

    /**
     * Logs information about successfully fetched JWK set including the number of keys retrieved, authentication
     * provider details, source URL, and expiration information.
     *
     * @param jwkInfo        the JWK info object containing the fetched keys and metadata
     * @param authProviderId the identifier of the authentication provider that owns the JWK set
     * @param url            the URL from which the JWK set was fetched
     * @param expirationInfo human-readable string describing the expiration status of the keys
     */
    private static void logJwkSetFetched(JwkInfo jwkInfo, String authProviderId, String url, String expirationInfo) {
        log.info("Fetched {} JWT public keys for auth provider '{}' from '{}'; {}",
                jwkInfo.keys().size(), authProviderId, url, expirationInfo);
    }

}
