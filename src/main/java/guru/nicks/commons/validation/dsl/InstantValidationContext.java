package guru.nicks.commons.validation.dsl;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.function.Predicate;

/**
 * Methods for validating an {@link Instant},
 */
@SuppressWarnings("UnusedReturnValue")
public class InstantValidationContext extends ValidationContext<Instant> implements Comparable<Instant> {

    public InstantValidationContext(@Nullable Instant value, String name) {
        super(value, name);
    }

    @Override // enforce return value type
    public InstantValidationContext notNull() {
        return (InstantValidationContext) super.notNull();
    }

    @Override // enforce return value type
    public InstantValidationContext constraint(Predicate<? super Instant> predicate, @Nullable String messageTemplate) {
        return (InstantValidationContext) super.constraint(predicate, messageTemplate);
    }

    /**
     * Checks that the instant is before the other one.
     *
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public InstantValidationContext before(@Nullable Instant other) {
        if (compareTo(other) >= 0) {
            formatAndThrow(ValidationMessage.BEFORE, other);
        }

        return this;
    }

    /**
     * Checks that the instant is before or equal to the other one.
     *
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public InstantValidationContext beforeOrEqual(@Nullable Instant other) {
        if (compareTo(other) > 0) {
            formatAndThrow(ValidationMessage.BEFORE_OR_EQUAL, other);
        }

        return this;
    }

    /**
     * Checks that the instant is after the other one.
     *
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public InstantValidationContext after(@Nullable Instant other) {
        if (compareTo(other) <= 0) {
            formatAndThrow(ValidationMessage.AFTER, other);
        }

        return this;
    }

    /**
     * Checks that the instant is after or equal to the other one.
     *
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public InstantValidationContext afterOrEqual(@Nullable Instant other) {
        if (compareTo(other) < 0) {
            formatAndThrow(ValidationMessage.AFTER_OR_EQUAL, other);
        }

        return this;
    }

    @Override
    public int compareTo(@Nullable Instant other) {
        notNull();

        // ensure nulls don't cause NPE in comparator
        if (other == null) {
            throw new IllegalArgumentException("Other value must not be null");
        }

        return getValue().compareTo(other);
    }

}
