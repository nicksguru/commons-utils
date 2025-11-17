package guru.nicks.commons.validation.dsl;

import jakarta.annotation.Nullable;

import java.util.function.Predicate;

/**
 * Methods for validating {@link Double},
 */
@SuppressWarnings("UnusedReturnValue")
public class DoubleValidationContext extends NumberValidationContext<Double> {

    public DoubleValidationContext(@Nullable Double value, String name) {
        super(value, name);
    }

    @Override // enforce return value type
    public DoubleValidationContext notNull() {
        return (DoubleValidationContext) super.notNull();
    }

    @Override // enforce return value type
    public DoubleValidationContext constraint(Predicate<? super Double> predicate, @Nullable String messageTemplate) {
        return (DoubleValidationContext) super.constraint(predicate, messageTemplate);
    }

    /**
     * Should not be used for direct equality comparison.
     */
    @Override
    public int compareToZero() {
        return compareTo(0.0);
    }

    @Override
    public int compareTo(@Nullable Double other) {
        notNull();

        // ensure nulls don't cause NPE in comparator
        if (other == null) {
            throw new IllegalArgumentException("Other value must not be null");
        }

        return getValue().compareTo(other);
    }

}
