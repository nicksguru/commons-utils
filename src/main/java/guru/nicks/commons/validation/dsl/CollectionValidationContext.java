package guru.nicks.commons.validation.dsl;

import jakarta.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * Methods for validating a {@link Collection},
 */
@SuppressWarnings("UnusedReturnValue")
public class CollectionValidationContext<T extends Collection<?>> extends ValidationContext<T> {

    public CollectionValidationContext(@Nullable T value, String name) {
        super(value, name);
    }

    @Override // enforce return value type
    public CollectionValidationContext<T> notNull() {
        return (CollectionValidationContext<T>) super.notNull();
    }

    @Override // enforce return value type
    public CollectionValidationContext<T> constraint(Predicate<? super T> predicate, @Nullable String messageTemplate) {
        return (CollectionValidationContext<T>) super.constraint(predicate, messageTemplate);
    }

    /**
     * Checks that the collection is not empty.
     *
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public CollectionValidationContext<T> notEmpty() {
        if (CollectionUtils.isEmpty(getValue())) {
            formatAndThrow(ValidationMessage.NOT_EMPTY);
        }

        return this;
    }

    /**
     * Checks that the collection is not null and its size is less than the given threshold.
     *
     * @param threshold threshold
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public CollectionValidationContext<T> sizeLessThan(int threshold) {
        checkNotNull(value -> value.size() < threshold, ValidationMessage.SIZE_LESS_THAN, threshold);
        return this;
    }

    /**
     * Checks that the collection is not null and its size is less than or equal to the given threshold.
     *
     * @param threshold threshold
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public CollectionValidationContext<T> sizeLessThanOrEqual(int threshold) {
        checkNotNull(value -> value.size() <= threshold, ValidationMessage.SIZE_LESS_THAN_OR_EQUAL, threshold);
        return this;
    }

    /**
     * Checks that the collection size is greater than the given threshold.
     *
     * @param threshold threshold
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public CollectionValidationContext<T> sizeGreaterThan(int threshold) {
        checkNotNull(value -> value.size() > threshold, ValidationMessage.SIZE_GREATER_THAN, threshold);
        return this;
    }

    /**
     * Checks that the collection size is greater than or equal to the given threshold.
     *
     * @param threshold threshold
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public CollectionValidationContext<T> sizeGreaterThanOrEqual(int threshold) {
        checkNotNull(value -> value.size() >= threshold, ValidationMessage.SIZE_GREATER_THAN_OR_EQUAL, threshold);
        return this;
    }

    /**
     * Checks that the collection is not null and its size is within the given range (inclusive).
     *
     * @param min minimum length
     * @param max maximum length
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public CollectionValidationContext<T> sizeBetweenInclusive(int min, int max) {
        checkNotNull(value -> (value.size() >= min) && (value.size() <= max),
                ValidationMessage.SIZE_BETWEEN_INCLUSIVE, min, max);

        return this;
    }

}
