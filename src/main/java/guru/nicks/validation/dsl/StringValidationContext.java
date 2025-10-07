package guru.nicks.validation.dsl;

import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Methods for validating a {@link String},
 */
@SuppressWarnings("UnusedReturnValue")
public class StringValidationContext extends ValidationContext<String> {

    public StringValidationContext(@Nullable String value, String name) {
        super(value, name);
    }

    @Override // enforce return value type
    public StringValidationContext notNull() {
        return (StringValidationContext) super.notNull();
    }

    @Override // enforce return value type
    public StringValidationContext constraint(Predicate<? super String> predicate, @Nullable String messageTemplate) {
        return (StringValidationContext) super.constraint(predicate, messageTemplate);
    }

    /**
     * Delegates validation to the given validator.
     *
     * @param validator validator
     * @return {@code this}
     */
    public StringValidationContext constraint(Consumer<? super StringValidationContext> validator) {
        validator.accept(this);
        return this;
    }

    /**
     * Checks that the string is not empty (whitespaces-only strings are OK).
     *
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public StringValidationContext notEmpty() {
        checkNotNull(StringUtils::isNotEmpty, ValidationMessage.NOT_EMPTY);
        return this;
    }

    /**
     * Checks that the string is not blank (blank means {@code null}/empty/whitespaces-only).
     *
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public StringValidationContext notBlank() {
        checkNotNull(StringUtils::isNotBlank, ValidationMessage.NOT_BLANK);
        return this;
    }

    /**
     * Checks that the string is not null and its length is less than the given threshold.
     *
     * @param threshold threshold
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public StringValidationContext shorterThan(int threshold) {
        checkNotNull(value -> value.length() < threshold, ValidationMessage.LENGTH_LESS_THAN, threshold);
        return this;
    }

    /**
     * Checks that the string is not null and its length is less than or equal to the given threshold.
     *
     * @param threshold threshold
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public StringValidationContext shorterThanOrEqual(int threshold) {
        checkNotNull(value -> value.length() <= threshold, ValidationMessage.LENGTH_LESS_THAN_OR_EQUAL, threshold);
        return this;
    }

    /**
     * Checks that the string is not null and its length is greater than the given threshold.
     *
     * @param threshold threshold
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public StringValidationContext longerThan(int threshold) {
        checkNotNull(value -> value.length() > threshold, ValidationMessage.LENGTH_GREATER_THAN, threshold);
        return this;
    }

    /**
     * Checks that the string is not null and its length is greater than or equal to the given threshold.
     *
     * @param threshold threshold
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public StringValidationContext longerThanOrEqual(int threshold) {
        checkNotNull(value -> value.length() >= threshold, ValidationMessage.LENGTH_GREATER_THAN_OR_EQUAL,
                threshold);
        return this;
    }

    /**
     * Checks that the string is not null and its length is within the given range (inclusive).
     *
     * @param min minimum length
     * @param max maximum length
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public StringValidationContext lengthBetweenInclusive(int min, int max) {
        checkNotNull(value -> (value.length() >= min) && (value.length() <= max),
                ValidationMessage.LENGTH_BETWEEN_INCLUSIVE, min, max);
        return this;
    }

}
