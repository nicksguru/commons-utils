package guru.nicks.commons.validation.dsl;

import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

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
     * Checks that the string is not empty (whitespaces-only strings are OK).
     *
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public StringValidationContext notEmpty() {
        notNullAnd(StringUtils::isNotEmpty, ValidationMessage.NOT_EMPTY);
        return this;
    }

    /**
     * Checks that the string is not blank (blank means {@code null}/empty/whitespaces-only).
     *
     * @return {@code this}
     * @throws IllegalArgumentException condition not met
     */
    public StringValidationContext notBlank() {
        notNullAnd(StringUtils::isNotBlank, ValidationMessage.NOT_BLANK);
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
        notNullAnd(value -> value.length() < threshold, ValidationMessage.LENGTH_LESS_THAN, threshold);
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
        notNullAnd(value -> value.length() <= threshold, ValidationMessage.LENGTH_LESS_THAN_OR_EQUAL, threshold);
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
        notNullAnd(value -> value.length() > threshold, ValidationMessage.LENGTH_GREATER_THAN, threshold);
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
        notNullAnd(value -> value.length() >= threshold, ValidationMessage.LENGTH_GREATER_THAN_OR_EQUAL,
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
        notNullAnd(value -> (value.length() >= min) && (value.length() <= max),
                ValidationMessage.LENGTH_BETWEEN_INCLUSIVE, min, max);
        return this;
    }

    /**
     * Checks that the string is not null and starts with the given prefix (case-sensitive).
     *
     * @param prefix prefix; must not be null or empty
     * @return {@code this}
     * @throws IllegalArgumentException prefix is null/empty or condition not met
     */
    public StringValidationContext startsWith(String prefix) {
        if (StringUtils.isEmpty(prefix)) {
            throw new IllegalArgumentException("prefix must not be null or empty");
        }

        notNullAnd(value -> value.startsWith(prefix), ValidationMessage.STARTS_WITH, prefix);
        return this;
    }

    /**
     * Checks that the string is not null and ends with the given suffix (case-sensitive).
     *
     * @param suffix suffix; must not be null or empty
     * @return {@code this}
     * @throws IllegalArgumentException suffix is null/empty or condition not met
     */
    public StringValidationContext endsWith(String suffix) {
        if (StringUtils.isEmpty(suffix)) {
            throw new IllegalArgumentException("suffix must not be null or empty");
        }

        notNullAnd(value -> value.endsWith(suffix), ValidationMessage.ENDS_WITH, suffix);
        return this;
    }

    /**
     * Checks that the string is not null and contains with the given substring (case-sensitive).
     *
     * @param substring substring; must not be null or empty
     * @return {@code this}
     * @throws IllegalArgumentException substring is null/empty or condition not met
     */
    public StringValidationContext contains(String substring) {
        if (StringUtils.isEmpty(substring)) {
            throw new IllegalArgumentException("substring must not be null or empty");
        }

        notNullAnd(value -> value.contains(substring), ValidationMessage.CONTAINS, substring);
        return this;
    }

}
