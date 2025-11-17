package guru.nicks.commons.validation.dsl;

import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Methods for validating {@link Integer},
 */
@SuppressWarnings("UnusedReturnValue")
public class IntegerValidationContext extends NumberValidationContext<Integer> {

    public IntegerValidationContext(@Nullable Integer value, String name) {
        super(value, name);
    }

    @Override // enforce return value type
    public IntegerValidationContext notNull() {
        return (IntegerValidationContext) super.notNull();
    }

    @Override // enforce return value type
    public IntegerValidationContext constraint(Predicate<? super Integer> predicate, @Nullable String messageTemplate) {
        return (IntegerValidationContext) super.constraint(predicate, messageTemplate);
    }

    /**
     * Checks if the number (even if it's {@code null}) equals the other value. This method is not in
     * {@link NumberValidationContext} because precise equality doesn't work for floating point numbers.
     *
     * @param other reference value
     * @return value
     * @throws IllegalArgumentException condition not met
     */
    public IntegerValidationContext eq(@Nullable Integer other) {
        if (!Objects.equals(getValue(), other)) {
            formatAndThrow(ValidationMessage.NUM_EQUAL, other);
        }

        return this;
    }

    @Override
    protected int compareToZero() {
        return compareTo(0);
    }

    @Override
    public int compareTo(@Nullable Integer other) {
        notNull();

        // ensure nulls don't cause NPE in comparator
        if (other == null) {
            throw new IllegalArgumentException("The other value must not be null");
        }

        return getValue().compareTo(other);
    }

}
