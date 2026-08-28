package guru.nicks.commons.utils.auth;

import guru.nicks.commons.auth.domain.BasicAuthCredentials;
import guru.nicks.commons.utils.crypto.HashUtils;

import am.ik.yavi.meta.ConstraintArguments;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.Strings;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;

@UtilityClass
public class AuthUtils {

    public static final String BEARER_AUTH_TYPE = "Bearer";

    /**
     * Header value prefix for Bearer auth.
     */
    public static final String BEARER_AUTH_PREFIX = BEARER_AUTH_TYPE + " ";

    /**
     * Checksum flavor markers: SHA256 (slow but cryptographic-grade), XXHash64 (very fast but not
     * cryptographic-grade, i.e. it's easy to invent an input string yielding the given hash value). The goal of
     * employing multiple algorithms is to avoid collisions (the checksum is used to deny access to blocked tokens,
     * which is a very sensitive decision). Thus, if one algorithm yields a collision for a token, the other does not
     * (for the same token).
     * <p>
     * WARNING: the resulting string must not contain '=', ':' and any other special characters because it's also used
     * as part of JMX bean search string.
     */
    private static final String SHA256_CHECKSUM_PREFIX = "sha256[";
    private static final String XXHASH64_CHECKSUM_MIDDLE = "]_xxh64[";
    private static final String CHECKSUM_SUFFIX = "]";

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

        // plain concatenation instead of String.format (which parses the template and allocates a Formatter per
        // call): '%s' of a String is locale-independent, so the output is byte-identical to the former format form
        return SHA256_CHECKSUM_PREFIX + HashUtils.SHA_256.computeHex(bytes)
                + XXHASH64_CHECKSUM_MIDDLE + HashUtils.XXHASH3.computeHex(bytes)
                + CHECKSUM_SUFFIX;
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

        // RFC 7617: password may contain ':' - split into at most 2 parts at the FIRST colon
        String[] parts = decoded.split(":", 2);
        check(parts.length, "number of Basic Auth header value parts").eq(2);

        return BasicAuthCredentials.builder()
                .username(parts[0])
                .password(parts[1])
                .build();
    }

}
