package guru.nicks.validation.dsl;

import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Methods for validating {@link Long},
 */
@SuppressWarnings("UnusedReturnValue")
public class LongValidationContext extends NumberValidationContext<Long> {

    public LongValidationContext(@Nullable Long value, String name) {
        super(value, name);
    }

    @Override // enforce return value type
    public LongValidationContext notNull() {
        return (LongValidationContext) super.notNull();
    }

    @Override // enforce return value type
    public LongValidationContext constraint(Predicate<? super Long> predicate, @Nullable String messageTemplate) {
        return (LongValidationContext) super.constraint(predicate, messageTemplate);
    }

    /**
     * Checks if the number (even if it's {@code null}) equals the other value. This method is not in
     * {@link NumberValidationContext} because precise equality doesn't work for floating point numbers.
     *
     * @param other reference value
     * @return value
     * @throws IllegalArgumentException condition not met
     */
    public LongValidationContext eq(@Nullable Long other) {
        if (!Objects.equals(getValue(), other)) {
            formatAndThrow(ValidationMessage.NUM_EQUAL, other);
        }

        return this;
    }

    @Override
    protected int compareToZero() {
        return compareTo(0L);
    }

    @Override
    public int compareTo(@Nullable Long other) {
        notNull();

        // ensure nulls don't cause NPE in comparator
        if (other == null) {
            throw new IllegalArgumentException("The other value must not be null");
        }

        return getValue().compareTo(other);
    }

}
