package guru.nicks.commons.auth.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * When used in {@code @ConfigurationProperties}, immutability doesn't work because this class has subclasses (the
 * configuration processor gets confused somehow).
 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldNameConstants
public class BasicAuthCredentials {

    public static final String BASIC_AUTH_TYPE = "Basic";

    /**
     * Header value prefix for Basic auth.
     *
     * @see #convertToHeaderValue()
     */
    public static final String BASIC_AUTH_PREFIX = BASIC_AUTH_TYPE + " ";

    @NotBlank
    private String username;
    @NotBlank
    private String password;

    /**
     * Checks if the {@link #getUsername() username} is not blank.
     *
     * @return {@code true} if username is not blank
     */
    public boolean hasUsername() {
        return StringUtils.isNotBlank(username);
    }

    /**
     * Converts the username and password to Basic Auth header value (with {@value #BASIC_AUTH_PREFIX} prepended).
     *
     * @return prefix plus Base64-encoded string in the format "username:password"
     * @throws IllegalArgumentException username and/or password is blank
     */
    public String convertToHeaderValue() {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("Username and password must not be blank");
        }

        String credentials = username + ":" + password;

        credentials = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));

        credentials = BASIC_AUTH_PREFIX + credentials;
        return credentials;
    }

}
