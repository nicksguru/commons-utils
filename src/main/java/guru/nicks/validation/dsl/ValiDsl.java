package guru.nicks.validation.dsl;

import jakarta.annotation.Nullable;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Validation-related methods. Throws {@link IllegalArgumentException} on invalid values - for {@code null} values too,
 * otherwise REST clients would see {@link NullPointerException} as HTTP status 500 ({@link IllegalArgumentException} is
 * supposed to be mapped to HTTP status 400).
 * <p>
 * Usage example leveraging {@link FieldNameConstants @FieldNameConstants}:
 * <pre>
 *  checkNonBlank(user.getFirstName(), User.Fields.firstName);
 *  checkNotNullNested(orderItem.getProduct(), "product", Product::getPrice, "price");
 *  int quantity = check(orderItem.getQuantity(), OrderItem.Fields.quantity).greaterThanOrEqual(1).getValue();
 *  check(duration, "duration").notNull().constraint(Duration::isPositive, "must be positive");
 * </pre>
 */
@UtilityClass
public class ValiDsl {

    /**
     * Speeds up the most common use case for objects (requiring a non-null object and returning it upon success).
     * Doesn't permit method chaining (doesn't create a {@link ValidationContext}), thus reducing GC pressure.
     *
     * @param value value to validate
     * @param name  field name for error messages
     * @param <T>   value type
     * @return non-null value
     * @throws IllegalArgumentException the value is {@code null}
     */
    public static <T> T checkNotNull(@Nullable T value, String name) {
        if (value == null) {
            String message = ValidationMessage.NOT_NULL.format(name);
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    /**
     * Checks that the given top-level value and its nested property are both non-null. Doesn't permit method chaining
     * (doesn't create a {@link ValidationContext}), thus reducing GC pressure.
     *
     * @param value1          top-level value
     * @param name1           top-level value name, e.g. 'product' (for error messages)
     * @param nestedExtractor function extracting the nested property from the top-level value
     * @param name2           nested property name, e.g. 'price' (the error message will mention 'product.price')
     * @param <T>             top-level value type
     * @param <R>             nested property type
     * @return non-null value
     * @throws IllegalArgumentException the top-level or nested value is {@code null}
     */
    public static <T, R> R checkNotNullNested(@Nullable T value1, String name1,
            Function<T, R> nestedExtractor, String name2) {
        checkNotNull(value1, name1);
        return checkNotNull(nestedExtractor.apply(value1), name1 + "." + name2);
    }

    /**
     * Speeds up the most common use case for strings (requiring a non-blank string and returning it upon success).
     * Doesn't permit method chaining (doesn't create a {@link StringValidationContext}), thus reducing GC pressure.
     *
     * @param value value to validate
     * @param name  field name
     * @throws IllegalArgumentException the value is {@code null}, or empty, or whitespaces-only
     * @see StringValidationContext#notBlank()
     */
    public static String checkNotBlank(String value, String name) {
        if (StringUtils.isBlank(value)) {
            String message = ValidationMessage.NOT_BLANK.format(name);
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    /**
     * Creates a context for the given {@link String}. This call doesn't validate anything by itself, it just returns a
     * set of validation methods available.
     *
     * @param value value to validate
     * @param name  field name
     */
    public static StringValidationContext check(@Nullable String value, String name) {
        return createContext(StringValidationContext::new, value, name);
    }

    /**
     * Creates a context for the given {@link Integer}. This call doesn't validate anything by itself, it just returns a
     * set of validation methods available.
     *
     * @param value value to validate
     * @param name  field name
     */
    public static IntegerValidationContext check(@Nullable Integer value, String name) {
        return createContext(IntegerValidationContext::new, value, name);
    }

    /**
     * Creates a context for the given {@link Long}. This call doesn't validate anything by itself, it just returns a
     * set of validation methods available.
     *
     * @param value value to validate
     * @param name  field name
     */
    public static LongValidationContext check(@Nullable Long value, String name) {
        return createContext(LongValidationContext::new, value, name);
    }

    /**
     * Creates a context for the given {@link Double}. This call doesn't validate anything by itself, it just returns a
     * set of validation methods available.
     *
     * @param value value to validate
     * @param name  field name
     */
    public static DoubleValidationContext check(@Nullable Double value, String name) {
        return createContext(DoubleValidationContext::new, value, name);
    }

    /**
     * Creates a context for the given {@link Instant}. This call doesn't validate anything by itself, it just returns a
     * set of validation methods available.
     *
     * @param value value to validate
     * @param name  field name
     */
    public static InstantValidationContext check(@Nullable Instant value, String name) {
        return createContext(InstantValidationContext::new, value, name);
    }

    /**
     * Creates a context for the given {@link Collection}. This call doesn't validate anything by itself, it just
     * returns a set of validation methods available.
     *
     * @param value value to validate
     * @param name  field name
     */
    public static <T extends Collection<?>> CollectionValidationContext<T> check(@Nullable T value, String name) {
        return createContext(CollectionValidationContext::new, value, name);
    }

    /**
     * Creates a context for the given object. This call doesn't validate anything by itself, it just returns a set of
     * validation methods available.
     * <p>
     * This is a fallback method for objects not covered by other same-named narrower-type methods.
     *
     * @param value value to validate
     * @param name  field name
     * @param <T>   value type
     * @return context
     */
    public static <T> ValidationContext<T> check(@Nullable T value, String name) {
        return createContext(ValidationContext::new, value, name);
    }

    private static <T, C extends ValidationContext<T>> C createContext(BiFunction<T, String, C> constructor,
            @Nullable T value, String name) {
        return constructor.apply(value, name);
    }

}
