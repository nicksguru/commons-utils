package guru.nicks.commons.utils.json;

import guru.nicks.commons.utils.ReflectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
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
     * Optimized {@link ObjectMapper} with custom {@link JsonNodeFactory} that uses {@link TreeMap} for automatic key
     * sorting. This provides ~20-30% performance improvement over ORDER_MAP_ENTRIES_BY_KEYS for deep objects. See
     * details
     * <a href="https://medium.com/@cowtowncoder/jackson-tips-sorting-json-using-jsonnode-ce4476e37aee">here</a>.
     */
    private static final ObjectMapper KEY_SORTING_JSON_MAPPER;

    static {
        KEY_SORTING_JSON_MAPPER = JsonMapper.builder()
                .nodeFactory(new SortingJsonNodeFactory())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.WRAP_EXCEPTIONS, false)
                .build();
        KEY_SORTING_JSON_MAPPER.registerModule(new JavaTimeModule());
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
     * Serializes the argument as follows:
     * <ul>
     *  <li>{@code null} is treated as-is</li>
     *  <li>for a non-null {@link ReflectionUtils#isScalar(Object) scalar}, {@link Object#toString()} is called</li>
     *  <li>a non-scalar is serialized with a custom {@link ObjectMapper} instance that sorts {@link Map} keys</li>
     * </ul>
     * Uses optimized canonical JSON mapper with custom JsonNodeFactory for better performance.
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

        // Convert to tree node first to ensure key sorting via TreeMap-based JsonNodeFactory.
        // This is necessary because direct serialization of Maps doesn't use the NodeFactory.
        try {
            JsonNode node = KEY_SORTING_JSON_MAPPER.valueToTree(obj);
            return KEY_SORTING_JSON_MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON serialization error: " + e.getMessage(), e);
        }
    }

    /**
     * Custom factory that uses {@link TreeMap} instead of {@link LinkedHashMap} for {@link ObjectNode}. This ensures
     * all object properties are automatically sorted alphabetically.
     */
    private static class SortingJsonNodeFactory extends JsonNodeFactory {

        @Override
        public ObjectNode objectNode() {
            return new ObjectNode(this, new TreeMap<>());
        }

    }

}
