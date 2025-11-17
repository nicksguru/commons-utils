package guru.nicks.commons.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Transformation-related utility methods.
 */
@UtilityClass
public class TransformUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            // process Java 8 dates
            .registerModule(new JavaTimeModule());

    /**
     * Converts the argument to a {@link Supplier} returning {@code null}. Useful for calling methods that require a
     * {@link Supplier}.
     *
     * @param code the code to be executed
     * @return supplier
     */
    public static Supplier<Void> toSupplier(Runnable code) {
        checkNotNull(code, "code");

        return () -> {
            code.run();
            return null;
        };
    }

    /**
     * Converts a {@link Consumer} to a {@link Function} returning {@code null}. Useful for calling methods that require
     * a {@link Function}.
     *
     * @param consumer the code to be executed
     * @return a {@link Function} that calls the consumer and returns {@code null}
     */
    public static <T, U> Function<T, U> toFunction(Consumer<T> consumer) {
        checkNotNull(consumer, "consumer");

        return (T t) -> {
            consumer.accept(t);
            return null;
        };
    }

    /**
     * Convenience method to {@link Iterable} to list by extracting/transforming its elements. Replaces the common
     * {@code something.stream().map(mapper).toList()} idiom.
     * <p>
     * For example, we may need class names from a list of classes, in which case {@code mapper} is
     * {@code Class::getName}.
     *
     * @param from   source list (the widest scope possible - {@link Iterable}), can be {@code null}
     * @param mapper mapping function (usage of {@link Function#andThen(Function)} is encouraged, but method references
     *               can't be chained like that directly)
     * @param <T>    source list type
     * @param <R>    mapped value type
     * @return mutable list - crucial for Hibernate if this list is assigned to another entity; if it's immutable,
     *         Hibernate can't save it because it tries to clear it first
     */
    public static <T, R> List<R> toList(@Nullable Iterable<T> from, Function<? super T, R> mapper) {
        if (from == null) {
            return new ArrayList<>();
        }

        checkNotNull(mapper, "mapper");

        return createStream(from)
                .map(mapper)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Maps {@link Iterable} to list by extracting/transforming its elements. Replaces the common
     * {@code something.stream().map(mapper1).map(mapper2).toList()} idiom.
     * <p>
     * For example, we may need class names (in uppercase) from a list of classes, in which case {@code mapper1} is
     * {@code Class::getName}, {@code mapper2} is {@code String::toUpperCase}. They can't be chained with
     * {@link Function#andThen(Function)} directly.
     *
     * @param from    source list (the widest scope possible - {@link Iterable}), can be {@code null}
     * @param mapper1 mapping function #1
     * @param mapper2 mapping function #2
     * @param <T>     source list type
     * @param <R>     mapped value #1 type
     * @param <U>     mapped value #2 type
     * @return mutable list - crucial for Hibernate if this list is assigned to another entity; if it's immutable,
     *         Hibernate can't save it because it tries to clear it first
     */
    public static <T, R, U> List<U> toList(@Nullable Iterable<T> from,
            Function<? super T, R> mapper1, Function<? super R, U> mapper2) {
        checkNotNull(mapper1, "mapper1");
        checkNotNull(mapper2, "mapper2");
        return toList(from, mapper1.andThen(mapper2));
    }

    /**
     * Maps {@link Iterable} to list by extracting/transforming its elements. Replaces the common
     * {@code something.stream().map(mapper1).map(mapper2).map(mapper3).toList()} idiom.
     *
     * @param from    source list (the widest scope possible - {@link Iterable}), can be {@code null}
     * @param mapper1 mapping function #1
     * @param mapper2 mapping function #2
     * @param mapper3 mapping function #3
     * @param <T>     source list type
     * @param <R>     mapped value #1 type
     * @param <U>     mapped value #2 type
     * @param <V>     mapped value #3 type
     * @return mutable list - crucial for Hibernate if this list is assigned to another entity; if it's immutable,
     *         Hibernate can't save it because it tries to clear it first
     */
    public static <T, R, U, V> List<V> toList(@Nullable Iterable<T> from,
            Function<? super T, R> mapper1, Function<? super R, U> mapper2, Function<? super U, V> mapper3) {
        checkNotNull(mapper1, "mapper1");
        checkNotNull(mapper2, "mapper2");
        checkNotNull(mapper3, "mapper3");
        return toList(from, mapper1.andThen(mapper2).andThen(mapper3));
    }

    /**
     * Maps {@link Iterable} to set by extracting/transforming its elements. Replaces the common
     * {@code something.stream().map(mapper).collect(Collectors.toSet())} idiom.
     * <p>
     * <p>
     * For example, we may need class names from a list of classes, in which case {@code mapper} is
     * {@link Class#getName}.
     *
     * @param from   source list (the widest scope possible - {@link Iterable}), can be {@code null}
     * @param mapper mapping function (usage of {@link Function#andThen(Function)} is encouraged, but method references
     *               can't be chained like that directly)
     * @param <T>    source list type
     * @param <R>    mapped value type
     * @return mutable set - crucial for Hibernate if this set is assigned to another entity; if it's immutable,
     *         Hibernate can't save it because it tries to clear it first
     */
    public static <T, R> Set<R> toSet(@Nullable Iterable<T> from, Function<? super T, R> mapper) {
        if (from == null) {
            return new HashSet<>();
        }

        checkNotNull(mapper, "mapper");

        return createStream(from)
                .map(mapper)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Maps {@link Iterable} to set by extracting/transforming its elements. Replaces the common
     * {@code something.stream().map(mapper1).map(mapper2).collect(Collectors.toSet())} idiom.
     * <p>
     * For example, we may need class names (in uppercase) from a list of classes, in which case {@code mapper1} is
     * {@code Class::getName}, {@code mapper2} is {@code String::toUpperCase}. They can't be chained with
     * {@link Function#andThen(Function)} directly.
     *
     * @param from    source list (the widest scope possible - {@link Iterable}), can be {@code null}
     * @param mapper1 mapping function #1
     * @param mapper2 mapping function #2
     * @param <T>     source list type
     * @param <R>     mapped value #1 type
     * @param <U>     mapped value #2 type
     * @return mutable set - crucial for Hibernate if this set is assigned to another entity; if it's immutable,
     *         Hibernate can't save it because it tries to clear it first
     */
    public static <T, R, U> Set<U> toSet(@Nullable Iterable<T> from,
            Function<? super T, R> mapper1, Function<? super R, U> mapper2) {
        checkNotNull(mapper1, "mapper1");
        checkNotNull(mapper2, "mapper2");
        return toSet(from, mapper1.andThen(mapper2));
    }

    /**
     * Maps {@link Iterable} to set by extracting/transforming its elements. Replaces the common
     * {@code something.stream().map(mapper1).map(mapper2).map(mapper3).collect(Collectors.toSet())} idiom.
     *
     * @param from    source list (the widest scope possible - {@link Iterable}), can be {@code null}
     * @param mapper1 mapping function #1
     * @param mapper2 mapping function #2
     * @param mapper3 mapping function #3
     * @param <T>     source list type
     * @param <R>     mapped value #1 type
     * @param <U>     mapped value #2 type
     * @param <V>     mapped value #3 type
     * @return mutable set - crucial for Hibernate if this set is assigned to another entity; if it's immutable,
     *         Hibernate can't save it because it tries to clear it first
     */
    public static <T, R, U, V> Set<V> toSet(@Nullable Iterable<T> from,
            Function<? super T, R> mapper1, Function<? super R, U> mapper2, Function<? super U, V> mapper3) {
        checkNotNull(mapper1, "mapper1");
        checkNotNull(mapper2, "mapper2");
        checkNotNull(mapper3, "mapper3");
        return toSet(from, mapper1.andThen(mapper2).andThen(mapper3));
    }

    /**
     * Stringifies the argument:
     * <ol>
     *     <li>{@code null} - as '' (<b>there are callers that depend on that!</b>)</li>
     *     <li>scalars (as per {@link ReflectionUtils#isScalar(Object)}) - as-is</li>
     *     <li>all the rest - via custom (i.e. not shared) {@link ObjectMapper}; its exceptions are masked with
     *         '[JSON ERROR]'</li>
     * </ol>
     * <p>
     * WARNING: as code profiling reveals, {@link ObjectMapper} is rather slow during tests, so it's better to avoid
     * calling this method on objects for logging purposes in time-critical code.
     *
     * @param obj             object to stringify, can be {@code null}
     * @param prettyPrintJson whether to pretty print JSON or create a one-liner
     * @return string ('[JSON ERROR]' on error)
     */
    public static String stringify(@Nullable Object obj, boolean prettyPrintJson) {
        if (obj == null) {
            return "";
        }

        if (ReflectionUtils.isScalar(obj)) {
            return obj.toString();
        }

        try {
            return prettyPrintJson
                    ? OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
                    : OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "[JSON ERROR]";
        }
    }

    /**
     * Does the same as {@link #stringify(Object, boolean)}, but with JSON pretty print disabled. Handy for use in
     * {@link Optional#map(Function)}.
     *
     * @param obj object to stringify, can be {@code null}
     * @return string
     */
    public static String stringify(@Nullable Object obj) {
        return stringify(obj, false);
    }

    private static <T> Stream<T> createStream(Iterable<T> from) {
        // native stream is probably faster
        return (from instanceof Collection<T> collection)
                ? collection.stream()
                : StreamSupport.stream(from.spliterator(), false);
    }

}
