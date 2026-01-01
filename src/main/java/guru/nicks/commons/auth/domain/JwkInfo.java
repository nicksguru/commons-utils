package guru.nicks.commons.auth.domain;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.Builder;

import java.time.Instant;

/**
 * JWKS (JSON Web Key Set) - JWK objects, as per <a href="https://datatracker.ietf.org/doc/html/rfc7517">IETF</a>. The
 * objects may expire, as per {@link #expirationDate()}, and must be re-fetched thereafter.
 */
@Builder(toBuilder = true)
public record JwkInfo(

        /**
         * Label for logging purposes.
         */
        String authProviderId,

        JWKSet keys,

        /**
         * Google, for example, returns this in {@code Cache-Control} response header. If not {@code null}, defines the
         * moment when the signature expires and therefore stops accepting any tokens.
         */
        Instant expirationDate) {
}
