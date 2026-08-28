package guru.nicks.commons.utils.json;

import guru.nicks.commons.utils.ReflectionUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
     * Canonicalizing {@link ObjectMapper}: object keys are sorted on the fly by {@link KeySortingGenerator} (single
     * serialization pass, no intermediate tree), while registered serializers canonicalize {@link Set} elements and
     * {@link BigDecimal} scale.
     */
    private static final ObjectMapper KEY_SORTING_JSON_MAPPER;

    static {
        KEY_SORTING_JSON_MAPPER = JsonMapper.builder()
                // Canonicalize Set values wherever they appear (top-level, POJO fields, Maps, nested).
                // BigDecimals are normalized the way the former tree-based canonicalization did.
                .addModule(new SimpleModule()
                        .addSerializer(new CanonicalSetSerializer())
                        .addSerializer(new CanonicalBigDecimalSerializer()))
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
     *  <li>a non-scalar is serialized in a single pass through {@link KeySortingGenerator}, which sorts object keys
     *      on the fly, while registered serializers sort {@link Set} elements as well</li>
     * </ul>
     *
     * @param obj object to encode (a {@link Set} which is not {@link SortedSet} is converted to a {@link TreeSet} to
     *            ensure predictable element order, but all elements must be {@link Comparable} in this case)
     * @return JSON / scalar / {@code null} (if the argument is {@code null})
     * @throws IllegalArgumentException JSON creation error
     * @throws ClassCastException       {@link TreeSet} failed to sort the original unsorted {@link Set} because its
     *                                  elements are not mutually comparable (see {@link TreeSet#TreeSet(Collection)}
     *                                  for details)
     */
    @Nullable
    public static String sortObjectKeys(@Nullable Object obj) {
        if (obj == null) {
            return null;
        }

        if (ReflectionUtils.isScalar(obj)) {
            return obj.toString();
        }

        // single pass: the value is serialized once, its object keys sorted on the fly by the generator
        try {
            var writer = new StringWriter();
            try (var generator = new KeySortingGenerator(
                    KEY_SORTING_JSON_MAPPER.getFactory().createGenerator(writer))) {
                generator.setCodec(KEY_SORTING_JSON_MAPPER);
                KEY_SORTING_JSON_MAPPER.writeValue(generator, sortSets(obj));
            }

            return writer.toString();
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON serialization error: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a top-level non-{@link SortedSet} {@link Set} to a {@link TreeSet}; sets nested inside POJO fields and
     * {@link Map} values are canonicalized by {@link CanonicalSetSerializer} registered on the mapper.
     *
     * @param obj object to canonicalize
     * @return canonicalized object (the argument itself if it's not a convertible set)
     * @throws ClassCastException set elements are not mutually comparable
     */
    private static Object sortSets(Object obj) {
        if (obj instanceof Set<?> set) {
            if (set instanceof SortedSet<?>) {
                return set;
            }

            // TreeSet constructor throws ClassCastException for non-Comparable elements - fail fast, as documented
            return new TreeSet<>(set);
        }

        return obj;
    }

    /**
     * Sorts {@link Set} values before serialization, so that two equal sets with different iteration order (a
     * {@link HashSet} versus a {@link LinkedHashSet}, or two sets built in different order) produce identical JSON
     * arrays - required for deterministic checksums and cache keys. Without it, Jackson serializes a set as an array in
     * its hash-based iteration order, which varies across JVM runs.
     *
     * @see #sortObjectKeys(Object)
     */
    private static final class CanonicalSetSerializer extends StdSerializer<Set<?>> {

        private CanonicalSetSerializer() {
            super(TypeFactory.defaultInstance().constructType(Set.class));
        }

        /**
         * Serializes the set as an array with elements in their natural order.
         *
         * @param value    set to serialize
         * @param gen      JSON generator to write the canonical array to
         * @param provider serializer provider
         * @throws IOException        JSON write error
         * @throws ClassCastException set elements are not mutually comparable
         */
        @Override
        public void serialize(Set<?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            // an already sorted set needs no copy; the TreeSet constructor fails fast on non-Comparable elements
            var canonical = (value instanceof SortedSet<?> sortedSet)
                    ? sortedSet
                    : new TreeSet<Object>(value);

            // serialize as List: a TreeSet lookup would match this Set serializer again and loop forever
            provider.defaultSerializeValue(new ArrayList<>(canonical), gen);
        }

    }

    /**
     * Normalizes {@link BigDecimal} values exactly the way the former tree-based canonicalization did (mirrors
     * {@code JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES} applied when numbers were converted to
     * {@link JsonNode}s): trailing zeros are stripped and any zero collapses to plain {@code 0}. Required to keep the
     * single-pass output byte-identical to the former two-pass one.
     */
    private static final class CanonicalBigDecimalSerializer extends StdSerializer<BigDecimal> {

        private CanonicalBigDecimalSerializer() {
            super(BigDecimal.class);
        }

        /**
         * Serializes the value with trailing zeros stripped.
         *
         * @param value    decimal to serialize
         * @param gen      JSON generator to write the normalized number to
         * @param provider serializer provider
         * @throws IOException JSON write error
         */
        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            // mirrors JsonNodeFactory.numberNode(BigDecimal): strip, and collapse any zero to plain 0
            var canonical = (value.signum() == 0) ? BigDecimal.ZERO : value.stripTrailingZeros();
            gen.writeNumber(canonical);
        }

    }

    /**
     * {@link JsonGenerator} wrapper that sorts object field names on the fly, replacing the former two-pass
     * canonicalization (build a sorted {@link JsonNode} tree, then serialize it). While an object is being written,
     * each field value is buffered in its own {@link TokenBuffer} guarded by a nested instance; when the object ends,
     * the buffers are replayed in {@link TreeMap} key order. Everything outside objects (arrays, scalars) passes
     * through untouched, so the output stays byte-identical to the tree-based one.
     */
    private static final class KeySortingGenerator extends JsonGeneratorDelegate {

        /**
         * Underlying generator this level writes completed content to; {@link #delegate} points at it between fields
         * and at the nested instance while a field value is being collected.
         */
        private final JsonGenerator output;

        /**
         * Completed fields of the object being collected at this level, always kept in sorted key order.
         */
        private final TreeMap<String, TokenBuffer> fields = new TreeMap<>();

        /**
         * Name of the field whose value is currently being collected, or {@code null} between fields. Sealed lazily:
         * the buffer moves into {@link #fields} only when the next field starts or the object ends.
         */
        private String pendingFieldName;

        /**
         * Buffer collecting the pending field's value.
         */
        private TokenBuffer pendingBuffer;

        /**
         * Nesting depth inside the pending field's value structure: {@code 0} until the value opens its own structure,
         * {@code >0} while inside it. When an end token brings it back to {@code 0}, the value is complete.
         */
        private int pendingValueDepth;

        /**
         * Whether an object is being collected at this level (as the top-level value or an array element).
         */
        private boolean collecting;

        private KeySortingGenerator(JsonGenerator delegate) {
            super(delegate);
            output = delegate;
        }

        /**
         * Routes the token into the pending field's structure; arrays never get their elements reordered.
         *
         * @throws IOException JSON write error
         */
        @Override
        public void writeStartArray() throws IOException {
            if (pendingFieldName != null) {
                pendingValueDepth++;
            }
            delegate.writeStartArray();
        }

        /**
         * Routes the token into the pending field's structure; arrays never get their elements reordered.
         *
         * @param size number of elements the array will have, if known
         * @throws IOException JSON write error
         */
        @Override
        public void writeStartArray(int size) throws IOException {
            writeStartArray();
        }

        /**
         * Routes the token into the pending field's structure; arrays never get their elements reordered.
         *
         * @param forValue value being written, used for generator bookkeeping only
         * @throws IOException JSON write error
         */
        @Override
        public void writeStartArray(Object forValue) throws IOException {
            writeStartArray();
        }

        /**
         * Routes the token into the pending field's structure; arrays never get their elements reordered.
         *
         * @param forValue value being written, used for generator bookkeeping only
         * @param size     number of elements the array will have, if known
         * @throws IOException JSON write error
         */
        @Override
        public void writeStartArray(Object forValue, int size) throws IOException {
            writeStartArray();
        }

        /**
         * Seals the pending field when its array value just closed, or passes the token through.
         *
         * @throws IOException JSON write error
         */
        @Override
        public void writeEndArray() throws IOException {
            if (pendingValueDepth > 0) {
                delegate.writeEndArray();

                if (--pendingValueDepth == 0) {
                    sealPendingField();
                }

                return;
            }

            delegate.writeEndArray();
        }

        /**
         * Starts collecting the object at this level, or routes the token into the pending field's structure.
         *
         * @throws IOException JSON write error
         */
        @Override
        public void writeStartObject() throws IOException {
            if (pendingFieldName != null) {
                delegate.writeStartObject();
                pendingValueDepth++;
            } else if (!collecting) {
                // an object in top-level or array-element position: collect and sort its fields
                collecting = true;
                fields.clear();
            } else {
                // a structure without a field name is not produced by well-formed serializers - pass through
                delegate.writeStartObject();
            }
        }

        /**
         * Starts collecting the object at this level, or routes the token into the pending field's structure.
         *
         * @param forValue value being written, used for generator bookkeeping only
         * @throws IOException JSON write error
         */
        @Override
        public void writeStartObject(Object forValue) throws IOException {
            writeStartObject();
        }

        /**
         * Starts collecting the object at this level, or routes the token into the pending field's structure.
         *
         * @param forValue value being written, used for generator bookkeeping only
         * @param size     number of fields the object will have, if known
         * @throws IOException JSON write error
         */
        @Override
        public void writeStartObject(Object forValue, int size) throws IOException {
            writeStartObject();
        }

        /**
         * Emits the collected fields in sorted order when the object at this level ends, or routes the token into the
         * pending field's structure and seals the field when its structure just closed.
         *
         * @throws IOException JSON write error
         */
        @Override
        public void writeEndObject() throws IOException {
            if (pendingValueDepth > 0) {
                delegate.writeEndObject();

                if (--pendingValueDepth == 0) {
                    // the pending field's structure just closed - its value is complete
                    sealPendingField();
                }

                return;
            }

            if (collecting) {
                if (pendingFieldName != null) {
                    // the last field held a scalar value
                    sealPendingField();
                }
                emitCollectedObject();
                return;
            }

            delegate.writeEndObject();
        }

        /**
         * Records the field name and starts collecting its value into a fresh buffer instead of writing it through.
         *
         * @param name field name
         * @throws IOException JSON write error
         */
        @Override
        public void writeFieldName(String name) throws IOException {
            if (pendingValueDepth > 0) {
                // a field of an object nested inside the pending field's value - belongs to the nested collector
                delegate.writeFieldName(name);
                return;
            }

            if (pendingFieldName != null) {
                // the previous field held a scalar value - seal it before starting the next one
                sealPendingField();
            }

            pendingFieldName = name;
            pendingBuffer = new TokenBuffer(getCodec(), false);
            delegate = new KeySortingGenerator(pendingBuffer);
        }

        /**
         * Records the field name and starts collecting its value into a fresh buffer instead of writing it through.
         *
         * @param name field name
         * @throws IOException JSON write error
         */
        @Override
        public void writeFieldName(SerializableString name) throws IOException {
            if (pendingValueDepth > 0) {
                delegate.writeFieldName(name);
            } else {
                writeFieldName(name.getValue());
            }
        }

        /**
         * Routes numeric field names (used for {@link Number} map keys) through the regular field handling; the
         * delegate implementation would bypass it and write the name unsorted.
         *
         * @param id field id
         * @throws IOException JSON write error
         */
        @Override
        public void writeFieldId(long id) throws IOException {
            writeFieldName(String.valueOf(id));
        }

        /**
         * Serializes the value through this generator so that object keys nested inside it are sorted too, instead of
         * letting the delegate serialize it into the buffer directly.
         *
         * @param value value to serialize
         * @throws IOException JSON write error
         */
        @Override
        public void writeObject(Object value) throws IOException {
            getCodec().writeValue(this, value);
        }

        /**
         * Serializes the tree through this generator so that object keys nested inside it are sorted too.
         *
         * @param tree tree to serialize
         * @throws IOException JSON write error
         */
        @Override
        public void writeTree(TreeNode tree) throws IOException {
            if (tree instanceof JsonNode) {
                // serialize through this generator so that object keys nested inside the tree are sorted too
                getCodec().writeValue(this, tree);
            } else {
                delegate.writeTree(tree);
            }
        }

        /**
         * Flushes the real output even while a pending field buffer is active.
         *
         * @throws IOException JSON flush error
         */
        @Override
        public void flush() throws IOException {
            output.flush();
        }

        /**
         * Closes the real output, not the buffer that happens to be active.
         *
         * @throws IOException JSON close error
         */
        @Override
        public void close() throws IOException {
            output.close();
        }

        /**
         * Moves the pending field's buffer into {@link #fields} and restores direct writing.
         */
        private void sealPendingField() {
            fields.put(pendingFieldName, pendingBuffer);
            pendingFieldName = null;
            pendingBuffer = null;
            pendingValueDepth = 0;
            delegate = output;
        }

        /**
         * Writes the collected fields of this level's object to the output in sorted key order.
         *
         * @throws IOException JSON write error
         */
        private void emitCollectedObject() throws IOException {
            output.writeStartObject();
            for (var entry : fields.entrySet()) {
                output.writeFieldName(entry.getKey());
                entry.getValue().serialize(output);
            }
            output.writeEndObject();
            fields.clear();
            collecting = false;
        }

    }

}
