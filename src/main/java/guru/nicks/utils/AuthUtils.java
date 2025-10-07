package guru.nicks.utils;

import guru.nicks.domain.BasicAuthCredentials;

import am.ik.yavi.meta.ConstraintArguments;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.Strings;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

import static guru.nicks.validation.dsl.ValiDsl.check;
import static guru.nicks.validation.dsl.ValiDsl.checkNotBlank;

@UtilityClass
public class AuthUtils {

    public static final String BEARER_AUTH_TYPE = "Bearer";

    /**
     * Header value prefix for Bearer auth.
     */
    public static final String BEARER_AUTH_PREFIX = BEARER_AUTH_TYPE + " ";

    /**
     * Each placeholder is a checksum flavor: SHA256 (slow but cryptographic-grade), XXHash64 (very fast but not
     * cryptographic-grade, i.e. it's easy to invent an input string yielding the given hash value). The goal of
     * employing multiple algorithms is to avoid collisions (the checksum is used to deny access to blocked tokens,
     * which is a very sensitive decision). Thus, if one algorithm yields a collision for a token, the other does not
     * (for the same token).
     * <p>
     * WARNING: the resulting string must not contain '=', ':' and any other special characters because it's also used
     * as part of JMX bean search string.
     */
    private static final String ACCESS_TOKEN_CHECKSUM_TEMPLATE = "sha256[%s]_xxh64[%s]";

    /**
     * For consistency, this method is the <b>only</b> one that knows how to calculate access token checksum based on
     * token's string representation (such as JWT). Two hash algorithms ensure that no hash collision occurs, i.e. no
     * other token will have the same SHA256 and XXHash64 checksums.
     *
     * @return checksum
     * @throws NullPointerException     {@code accessTokenValue} is null
     * @throws IllegalArgumentException {@code accessTokenValue} is empty or whitespace-only
     */
    @ConstraintArguments
    public static String calculateAccessTokenChecksum(String accessTokenValue) {
        checkNotBlank(accessTokenValue, _AuthUtilsCalculateAccessTokenChecksumArgumentsMeta.ACCESSTOKENVALUE.name());
        byte[] bytes = accessTokenValue.getBytes(StandardCharsets.UTF_8);

        return String.format(Locale.US, ACCESS_TOKEN_CHECKSUM_TEMPLATE,
                HashUtils.SHA_256.computeHex(bytes),
                HashUtils.XXHASH3.computeHex(bytes));
    }

    /**
     * Parses {@code Authorization: Basic ...} header into username and password.
     *
     * @param header header
     * @return username and password
     * @throws IllegalArgumentException invalid header: blank, non-Basic, malformed, etc.
     */
    @ConstraintArguments
    public static BasicAuthCredentials parseBasicAuthHeader(String header) {
        check(header, _AuthUtilsParseBasicAuthHeaderArgumentsMeta.HEADER.name())
                .notBlank()
                // prefix plus something else (header value)
                .longerThan(BasicAuthCredentials.BASIC_AUTH_PREFIX.length())
                .constraint(str -> str.startsWith(BasicAuthCredentials.BASIC_AUTH_PREFIX), "has invalid prefix");

        String headerValue = Strings.CS.removeStart(header, BasicAuthCredentials.BASIC_AUTH_PREFIX);

        String decoded = new String(
                Base64.getDecoder().decode(headerValue),
                StandardCharsets.UTF_8);

        String[] parts = decoded.split(":");
        check(parts.length, "number of Basic Auth header value parts").eq(2);

        return BasicAuthCredentials.builder()
                .username(parts[0])
                .password(parts[1])
                .build();
    }

}
