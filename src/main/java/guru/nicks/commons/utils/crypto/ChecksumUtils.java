package guru.nicks.commons.utils.crypto;

import guru.nicks.commons.utils.json.JsonUtils;
import guru.nicks.commons.validation.dsl.ValiDsl;

import am.ik.yavi.meta.ConstraintArguments;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

@UtilityClass
@Slf4j
public class ChecksumUtils {

    /**
     * NOTE: {@code matcher.matches()} needs a regexp covering the whole string; {@code matcher.find()} does not.
     */
    private static final Predicate<String> ALL_DECIMALS_PREDICATE = Pattern.compile("^\\d+$").asMatchPredicate();

    private static final Function<byte[], String> BINARY_ENCODER = Base64.getEncoder()::encodeToString;

    /**
     * Computes checksum by first serializing the given object and then feeding it to SHA-256. For caveats and
     * exceptions thrown, see {@link JsonUtils#sortObjectKeys(Object) serializer}.
     * <p>
     * WARNING: to store a checksum in DB, column text-sensitivity must be ensured because Base64 is case-sensitive:
     * <ul>
     *  <li>for MySQL: {@code checksum VARCHAR(255) COLLATE utf8mb4_bin}</li>
     *  <li>PostgreSQL: {@code checksum VARCHAR(255) COLLATE "C"}</li>
     * </ul>
     *
     * @param obj the object (or a boxed primitive) to compute checksum for ({@code null} is treated as an empty string,
     *            for usability purposes and to avoid returning nulls which would otherwise be wrapped in double quotes
     *            in some use cases)
     * @return Base64-encoded checksum
     */
    public static String computeJsonChecksumBase64(@Nullable Object obj) {
        String serialized = (obj == null)
                ? ""
                : JsonUtils.sortObjectKeys(obj);
        byte[] checksum = DigestUtils.sha256(serialized);
        return BINARY_ENCODER.apply(checksum);
    }

    /**
     * A check digit originally accepts decimal strings only and produces a decimal digit. This method extends that to
     * arbitrary input strings and alphabets: decimal strings are processed as-is (to retain compatibility with credit
     * card numbers), non-decimal ones are transformed to their Unicode codepoints (Unicode as 1 114 112 codepoints).
     * <p>
     * The check digit (or a smaller/bigger integer) is then projected onto the given alphabet. For example, if the
     * alphabet is 'abc' and the digit is 1, the checksum is 'b'; the digit 3 becomes 'a' because the alphabet is too
     * short and rollover is performed.
     * <p>
     * If {@code algorithm} returns a digit, only the first 10 characters of the alphabet are used.
     * <p>
     * WARNING: the checksum should not be treated as crypto grade, it's just a way to validate something before looking
     * it up in DB - to reduce DB pressure.
     *
     * @param payload   string to compute checksum for
     * @param algorithm returns byte representation of a {@link String} holding any integer (negative values will be
     *                  inverted, the number length is unlimited), for example {@link HashUtils#LUHN_DIGIT} (doesn't
     *                  permit all-zero input) or {@link HashUtils#VERHOEFF} which, unlike modulo-based algorithms,
     *                  accepts all-zero input, but permits decimal digits only
     * @param alphabet  alphabet to map the integer checksum on
     * @return a character belonging to {@code alphabet}
     * @throws IllegalArgumentException {@code payload} is {@code null} or ''; or {@code algorithm} is {@code null}; or
     *                                  {@code alphabet} is blank or contains duplicate characters
     * @throws NumberFormatException    the algorithm didn't return a non-negative integer
     */
    @ConstraintArguments
    public static char computeExtendedCheckDigit(String payload, UnaryOperator<byte[]> algorithm, String alphabet) {
        ValiDsl.check(payload, _ChecksumUtilsComputeExtendedCheckDigitArgumentsMeta.PAYLOAD.name()).notEmpty();
        checkNotNull(algorithm, _ChecksumUtilsComputeExtendedCheckDigitArgumentsMeta.ALGORITHM.name());
        check(alphabet, _ChecksumUtilsComputeExtendedCheckDigitArgumentsMeta.ALPHABET.name())
                .notBlank()
                .constraint(str -> str.chars().distinct().count() == str.length(), "contains duplicate characters");

        String digits;
        // use decimal strings as-is - Luhn algo requires exactly such strings to verify credit card numbers (throws an
        // exception on non-decimal input)
        if (ALL_DECIMALS_PREDICATE.test(payload)) {
            digits = payload;
        } else {
            digits = payload
                    .codePoints()
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(""));
        }

        BigInteger checksumAsInt = Optional.of(digits.getBytes(StandardCharsets.UTF_8))
                .map(algorithm)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .map(BigInteger::new)
                .map(BigInteger::abs)
                .orElseThrow(() -> new IllegalArgumentException("Missing check digit"));

        int indexInAlphabet = checksumAsInt
                .mod(BigInteger.valueOf(alphabet.length()))
                .intValue();
        return alphabet.charAt(indexInAlphabet);
    }

    /**
     * Validates the checksum generated with {@link #computeExtendedCheckDigit(String, UnaryOperator, String)}. Note
     * that Luhn algo throws {@link IllegalArgumentException} on all-zero input.
     *
     * @param value     payload with checksum
     * @param algorithm same as in {@link #computeExtendedCheckDigit(String, UnaryOperator, String)}
     * @param alphabet  same as in {@link #computeExtendedCheckDigit(String, UnaryOperator, String)}
     * @return {@code false} if the payload is {@code null}, or shorter than 2 characters, or has an invalid checksum
     * @throws IllegalArgumentException {@code algorithm} is {@code null}; or {@code alphabet} is blank or contains
     *                                  duplicate characters
     */
    @ConstraintArguments
    public static boolean isValidExtendedCheckDigit(@Nullable String value, UnaryOperator<byte[]> algorithm,
            String alphabet) {
        // no room for check digit
        if ((value == null) || (value.length() < 2)) {
            return false;
        }

        String payload = value.substring(0, value.length() - 1);
        char actualChecksum = computeExtendedCheckDigit(payload, algorithm, alphabet);

        char expectedChecksum = value.charAt(value.length() - 1);
        return actualChecksum == expectedChecksum;
    }

}
