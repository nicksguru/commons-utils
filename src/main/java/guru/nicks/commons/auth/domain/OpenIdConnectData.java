package guru.nicks.commons.auth.domain;

import java.util.Locale;

/**
 * Open ID Connect data - typically stored in JWT by auth providers. Contains some of the
 * <a href="https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims">standard claims</a>.
 */
public interface OpenIdConnectData {

    /**
     * This is in fact a user ID, but called 'ID' for convenient mapping: in databases, it becomes the primary key.
     * <p>
     * WARNING: this user ID must be globally unique, i.e. must be post-processed already. Namely,
     * {@link JwtProvider#getCustomUserIdPrefix() prefix} must be prepended, otherwise user IDs assigned by external
     * auth providers may clash. For local users, this can be an stringified UUID.
     *
     * @return user ID post-processed according to the logic described above
     */
    String getId();

    String getUsername();

    String getEmail();

    /**
     * True if authentication provider claims that {@link #getEmail()} has been verified, usually by sending a message
     * with a verification link or code.
     *
     * @return email verified flag
     */
    boolean isEmailVerified();

    /**
     * 2-letter ISO code, such as 'en'. Not {@link Locale} to avoid failing on arbitrary, even erroneous, values.
     */
    String getLanguageCode();

    String getFirstName();

    String getLastName();

    /**
     * This is not necessarily the same as {@link #getFirstName()} + {@link #getFirstName()} - reported by auth
     * providers networks separately.
     */
    String getFullName();

    /**
     * Presumably a {@link java.net.URL}, but generally comes from JWT, so there's no formal guarantee.
     */
    String getPictureLink();

}
