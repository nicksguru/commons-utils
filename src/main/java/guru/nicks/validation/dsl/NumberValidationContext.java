package guru.nicks.validation.dsl;

import jakarta.annotation.Nullable;

/**
 * Methods for validating numbers. Comparisons fail for nulls because such is the contract of
 * {@link Comparable#compareTo(Object)}.
 */
@SuppressWarnings("UnusedReturnValue")
public abstract class NumberValidationContext<T extends Number> extends ValidationContext<T> implements Comparable<T> {

    protected NumberValidationContext(@Nullable T value, String name) {
        super(value, name);
    }

    /**
     * Checks if the number is positive. If the value is {@code null}, comparison fails, as per
     * {@link Comparable#compareTo(Object)}.
     *
     * @return value
     * @throws IllegalArgumentException condition not met
     */
    public NumberValidationContext<T> positive() {
        if (compareToZero() <= 0) {
            formatAndThrow(ValidationMessage.NUM_POSITIVE);
        }

        return this;
    }

    /**
     * Checks if the number is positive or zero. If the value is {@code null}, comparison fails, as per
     * {@link Comparable#compareTo(Object)}.
     *
     * @return value
     * @throws IllegalArgumentException condition not met
     */
    public NumberValidationContext<T> positiveOrZero() {
        if (compareToZero() < 0) {
            formatAndThrow(ValidationMessage.NUM_POSITIVE_OR_ZERO);
        }

        return this;
    }

    /**
     * Checks if the number is less than the given other. If at least one number is {@code null}, comparison fails, as
     * per {@link Comparable#compareTo(Object)}.
     *
     * @param other value to compare to
     * @return value
     * @throws IllegalArgumentException condition not met
     */
    public NumberValidationContext<T> lessThan(T other) {
        if (compareTo(other) >= 0) {
            formatAndThrow(ValidationMessage.NUM_LESS_THAN, other);
        }

        return this;
    }

    /**
     * Checks if the number is less than or equal to the other one. If at least one number is {@code null}, comparison
     * fails, as per {@link Comparable#compareTo(Object)}.
     *
     * @param other value to compare to
     * @return value
     * @throws IllegalArgumentException condition not met
     */
    public NumberValidationContext<T> lessThanOrEqual(T other) {
        if (compareTo(other) > 0) {
            formatAndThrow(ValidationMessage.NUM_LESS_THAN_OR_EQUAL, other);
        }

        return this;
    }

    /**
     * Checks if the number is greater than the given other. If at least one number is {@code null}, comparison fails,
     * as per {@link Comparable#compareTo(Object)}.
     *
     * @param other value to compare to
     * @return value
     * @throws IllegalArgumentException condition not met
     */
    public NumberValidationContext<T> greaterThan(T other) {
        if (compareTo(other) <= 0) {
            formatAndThrow(ValidationMessage.NUM_GREATER_THAN, other);
        }

        return this;

    }

    /**
     * Checks if the number is greater than or equal to the other one. If at least one number is {@code null},
     * comparison fails, as per {@link Comparable#compareTo(Object)}.
     *
     * @param other value to compare to
     * @return value
     * @throws IllegalArgumentException condition not met
     */
    public NumberValidationContext<T> greaterThanOrEqual(T other) {
        if (compareTo(other) < 0) {
            formatAndThrow(ValidationMessage.NUM_GREATER_THAN_OR_EQUAL, other);
        }

        return this;
    }

    /**
     * Checks if the number is within the given range (inclusive). If at least one number is {@code null}, comparison
     * fails, as per {@link Comparable#compareTo(Object)}.
     *
     * @param min minimum value
     * @param max maximum value
     * @return value
     * @throws IllegalArgumentException condition not met
     */
    public NumberValidationContext<T> betweenInclusive(T min, T max) {
        // comparison throws IllegalArgumentException if this and/or the other value is null
        if ((compareTo(min) < 0) || (compareTo(max) > 0)) {
            formatAndThrow(ValidationMessage.NUM_BETWEEN_INCLUSIVE, min, max);
        }

        return this;
    }

    /**
     * The concept of zero differs in subclasses.
     *
     * @return result of {@link #compareTo(Object)}, the other object being a zero of type {@code T}
     */
    protected abstract int compareToZero();

}
