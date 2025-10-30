package guru.nicks.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Text-related utility methods.
 */
@UtilityClass
public class JsonUtils {

    /**
     * Become part of regexp (case-insensitive). Masks field values whose names contain these substrings.
     */
    @SuppressWarnings("java:S5843") // long regexp
    private static final String SENSITIVE_JSON_FIELD_NAME_PARTS =
            "username|firstName|lastName|middleName|givenName|fullName|surname|patronymic"
                    + "|birth|mail|contact|home|origin|passport|pasport"
                    + "|addres|adres|city|town|street|house|apart|apt|state|county|zip|post"
                    + "|phone|gsm|cellular|mobile"
                    + "|passw|pw|pwd|auth|social|secur|credit|tok"
                    + "|card|salar|wage|incom";

    private static final Pattern SENSITIVE_JSON_FIELD_NAME_MASK_PATTERN = Pattern.compile(String.format(Locale.US, """
            (?xis)
            # "field name"
            (" [^"]* (?:%s) [^"]* ")
            
            # separator
            (\\p{javaSpaceChar}*:\\p{javaSpaceChar}*)
            
            # "field value", or true/false, or +/-12.34 (only one '.' is allowed in numbers, but it doesn't matter here)
            ("[^"]*" | true | false | [+-]?[\\d.]+)
            """, SENSITIVE_JSON_FIELD_NAME_PARTS));

    private static final String SENSITIVE_JSON_FIELD_REPLACEMENT = "**MASKED**";

    /**
     * Private object with predictable features crucial for consistent checksum computation. Map keys are sorted - to
     * avoid checksum differences caused by random key order (for this to work, objects must first be serialized to maps
     * and then to JSON)
     * <p>
     * Also, dates are written as timestamps, for consistency. {@link ObjectMapper} bean is not used because it may or
     * may not be configured to sort keys.
     */
    private static final ObjectMapper KEY_SORTING_OBJECT_MAPPER = new ObjectMapper()
            // sort keys in JSON to render unsorted maps, such as HashMap, in consistent order
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            // process Java 8 dates
            .registerModule(new JavaTimeModule());

    /**
     * Replaces {@link #SENSITIVE_JSON_FIELD_NAME_PARTS} with {@value #SENSITIVE_JSON_FIELD_REPLACEMENT}.
     * <p>
     * WARNING: after masking, the string is not a valid JSON - field values lose their wrapping in quotes. The reason
     * is that booleans and numbers don't have wrapping initially, strings do - it's hard to keep consistency.
     *
     * @param json JSON
     * @return JSON with some fields possibly masked ({@code null} if the argument is {@code null})
     */
    @Nullable
    public static String maskSensitiveJsonFields(@Nullable String json) {
        if (StringUtils.isBlank(json)) {
            return json;
        }

        return SENSITIVE_JSON_FIELD_NAME_MASK_PATTERN
                .matcher(json)
                .replaceAll("$1$2" + SENSITIVE_JSON_FIELD_REPLACEMENT);
    }

    /**
     * Replaces {@link #SENSITIVE_JSON_FIELD_NAME_PARTS} with {@value #SENSITIVE_JSON_FIELD_REPLACEMENT}.
     * <p>
     * WARNING: after masking, the string is not a valid JSON - field values lose their wrapping in quotes. The reason
     * is that booleans and numbers don't have wrapping initially, strings do - it's hard to keep consistency.
     *
     * @param json JSON
     * @return JSON with some fields possibly masked ({@code null} if the argument is {@code null})
     */
    @Nullable
    public static String maskSensitiveJsonFields(@Nullable byte[] json) {
        if ((json == null) || (json.length == 0)) {
            return null;
        }

        String str = new String(json, StandardCharsets.UTF_8);
        return maskSensitiveJsonFields(str);
    }

    /**
     * Processes the argument as follows:
     * <ul>
     *  <li>{@code null} is treated as-is</li>
     *  <li>for a non-null {@link ReflectionUtils#isScalar(Object) scalar}, {@link Object#toString()} is called</li>
     *  <li>a non-scalar is serialized with a custom {@link ObjectMapper} instance that sorts {@link Map} keys</li>
     * </ul>
     *
     * @param obj object to encode (a {@link Set} which is not {@link SortedSet} is converted to a {@link TreeSet} to
     *            ensure predictable key order, but all elements must be {@link Comparable} in this case)
     * @return JSON / scalar / {@code null} (if the argument is {@code null})
     * @throws IllegalArgumentException JSON creation error
     * @throws NullPointerException     if {@link TreeSet} failed to sort the original unsorted {@link Set} (see
     *                                  {@link TreeSet#addAll(Collection)} for details)
     */
    @Nullable
    public static String sortObjectKeys(@Nullable Object obj) {
        if (obj == null) {
            return null;
        }

        if (ReflectionUtils.isScalar(obj)) {
            return obj.toString();
        }

        if (obj instanceof Collection<?>) {
            // for Java pre-21, use: '&& !SortedSet && !LinkedHashSet')
            if ((obj instanceof Set<?> set) && !(obj instanceof SequencedSet)) {
                obj = new TreeSet<>(set);
            }

            try {
                return KEY_SORTING_OBJECT_MAPPER.writeValueAsString(obj);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("JSON serialization error: " + e.getMessage(), e);
            }
        }

        Map<?, ?> mapWithSortedKeys = KEY_SORTING_OBJECT_MAPPER.convertValue(obj, Map.class);

        try {
            return KEY_SORTING_OBJECT_MAPPER.writeValueAsString(mapWithSortedKeys);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON serialization error: " + e.getMessage(), e);
        }
    }

}
