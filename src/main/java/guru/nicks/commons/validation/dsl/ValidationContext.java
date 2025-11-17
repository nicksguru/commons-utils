package guru.nicks.commons.validation.dsl;

import jakarta.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Validation context. It's immutable and designed for method chaining: each method - including subclasses - must return
 * {@code this}.
 *
 * @param <T> value type
 */
@Getter
@ToString
@EqualsAndHashCode
public class ValidationContext<T> {

    // @Nullable - commented out because getValue() goes after notNull() in most cases
    private final T value;
    private final String name;

    /**
     * Creates a new validation context with the given value and name.
     *
     * @param value the value to validate, can be {@code null}
     * @param name  the name of the value (for error messages), must not be blank
     * @throws IllegalArgumentException name is blank
     */
    public ValidationContext(@Nullable T value, String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException(ValidationMessage.NOT_BLANK.format("name"));
        }

        this.value = value;
        this.name = name;
    }

    /**
     * Checks that the value is not {@code null}. Subclasses must override this method (just call {@code super}) for
     * returning their concrete {@code T} - for method chaining.
     * <p>
     * If this is the only check (i.e. no method chaining is needed), consider calling
     * {@link ValiDsl#checkNotNull(Object, String)}.
     *
     * @return {@code this}
     * @throws IllegalArgumentException the value is {@code null}
     */
    public ValidationContext<T> notNull() {
        if (value == null) {
            formatAndThrow(ValidationMessage.NOT_NULL);
        }

        return this;
    }

    /**
     * Passes the value (possibly {@code null}) to the given predicate. Subclasses must override this method (just call
     * {@code super}) for returning their concrete {@code T} - for method chaining.
     * <p>
     * Before calling this method, please take a look at build-in methods, for example, in
     * {@link StringValidationContext}.
     *
     * @param predicate       predicate
     * @param messageTemplate For format, see e.g. {@link ValidationMessage#NUM_BETWEEN_INCLUSIVE}. If it's blank, it
     *                        becomes {@code %s}. Otherwise, if it doesn't contain {@code %s}, which is a placeholder
     *                        for {@link #getName()}, {@code %s} (and a trailing whitespace) is prepended.
     * @return {@code this}
     * @throws IllegalArgumentException the predicate returned {@code false}
     */
    public ValidationContext<T> constraint(Predicate<? super T> predicate, @Nullable String messageTemplate) {
        Objects.requireNonNull(predicate, "predicate");

        if (predicate.test(value)) {
            return this;
        }

        if (StringUtils.isBlank(messageTemplate)) {
            messageTemplate = ValidationMessage.DECORATED_NAME_PLACEHOLDER;
        }

        // ensure there's a placeholder for the property name
        if (!messageTemplate.contains(ValidationMessage.RAW_NAME_PLACEHOLDER)) {
            messageTemplate = ValidationMessage.DECORATED_NAME_PLACEHOLDER + " " + messageTemplate;
        }

        throw new IllegalArgumentException(String.format(messageTemplate, name, value));
    }

    /**
     * Throws {@link IllegalArgumentException} with the given message if the value is {@code null} or the condition is
     * not met.
     *
     * @param predicate   predicate accepting the value (already checked for being non-null)
     * @param message     error message template
     * @param messageArgs arguments for the message template ({@link #getName()} is prepended implicitly)
     */
    protected void checkNotNull(Predicate<? super T> predicate, ValidationMessage message, Object... messageArgs) {
        notNull();

        if (!predicate.test(value)) {
            formatAndThrow(message, messageArgs);
        }
    }

    /**
     * Formats the message and throws a {@link IllegalArgumentException}.
     *
     * @param message message template
     * @param args    arguments for the template ({@link #getName()} is prepended implicitly, don't pass it)
     * @throws IllegalArgumentException in all cases
     */
    protected void formatAndThrow(ValidationMessage message, Object... args) {
        Object[] values = new Object[args.length + 1];

        Arrays.setAll(values, i -> (i == 0)
                // prepend property name
                ? name
                // fix all nulls, otherwise '%s' formatting fails for them
                : (args[i - 1] == null)
                        ? "<null>"
                        : args[i - 1]);

        String msg = message.format(values);
        throw new IllegalArgumentException(msg);
    }

}
