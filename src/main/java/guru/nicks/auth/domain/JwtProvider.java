package guru.nicks.auth.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.ValidationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * External (relative to this project) JWT providers, such as Google.
 * <p>
 * WARNING: this enum only works for users who present external tokens (issued by such auth providers as Google)
 * <b>directly</b> to our REST API. If external auth providers are attached to Keycloak or similar software, the
 * original tokens are substituted with Keycloak-generated ones. In such tokens, the original token issuer is missing -
 * all users are treated as <b>local</b>.
 */
@RequiredArgsConstructor
@Getter
public enum JwtProvider {

    GOOGLE("https://accounts.google.com", "google_");

    /**
     * @see #findByJwtIssuer(String)
     */
    private static final Map<String, JwtProvider> BY_ISSUER = Arrays.stream(values())
            .collect(Collectors.toMap(JwtProvider::getIssClaim, it -> it,
                    (existingOrigin, replacementOrigin) -> {
                        throw new IllegalStateException("Duplicate JWT issuer in "
                                + existingOrigin + " and " + replacementOrigin);
                    }));

    /**
     * JWT {@code ISS} claim
     */
    private final String issClaim;

    /**
     * For each auth provider, prepend this prefix to their local user IDs ({@code sub} JWT claim) - to make user IDs
     * unique, to avoid collisions of possibly identical user IDs reported by different origins.
     */
    private final String customUserIdPrefix;

    /**
     * Finds enum member by its {@link JwtProvider#getIssClaim()}.
     *
     * @param jwtIssuer JWT ISS claim value to look up (can be {@code null})
     * @return optional enum member
     */
    public static Optional<JwtProvider> findByJwtIssuer(String jwtIssuer) {
        return Optional.ofNullable(BY_ISSUER.get(jwtIssuer));
    }

    /**
     * Calls {@link JwtProvider#valueOf(String)}, wrapping the error in a {@link ValidationException} whose cause is a
     * {@link BindException} - for rendering in HTTP responses as a {@link FieldError}.
     *
     * @param value one of {@link JwtProvider#values()}
     * @return enum member
     * @throws ValidationException no such value
     */
    @JsonCreator
    public static JwtProvider of(String value) {
        try {
            return JwtProvider.valueOf(value);
        } catch (IllegalArgumentException e) {
            // exact field name is unknown, so this is just a sample (perhaps real) value
            var fieldError = new FieldError("request", "jwtProvider", value, true,
                    new String[]{"NoSuchJwtProvider"}, null, "No such JWT provider");

            // binding target is unknown (and doesn't matter)
            var result = new BeanPropertyBindingResult("", "request");
            result.addError(fieldError);
            throw new ValidationException(new BindException(result));
        }
    }

}
