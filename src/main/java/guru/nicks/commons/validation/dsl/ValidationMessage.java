package guru.nicks.commons.validation.dsl;

import lombok.Getter;

import java.util.Locale;

/**
 * In each message template, the 1st placeholder is for {@link ValidationContext#getName()}; it's prepended implicitly.
 */
@Getter
public enum ValidationMessage {

    NOT_NULL("must not be null"),

    /**
     * For strings and collections.
     */
    NOT_EMPTY("must not be empty"),

    /**
     * For strings only.
     */
    NOT_BLANK("must not be blank"),
    //
    LENGTH_LESS_THAN("length must be less than %d"),
    LENGTH_LESS_THAN_OR_EQUAL("length must be less than or equal to %d"),
    //
    LENGTH_GREATER_THAN("length must be greater than %d"),
    LENGTH_GREATER_THAN_OR_EQUAL("length must be greater than or equal to %d"),
    //
    LENGTH_BETWEEN_INCLUSIVE("length must be between %d and %d (inclusive)"),

    /**
     * For collections only.
     */
    SIZE_LESS_THAN("size must be less than %d"),
    SIZE_LESS_THAN_OR_EQUAL("length must be less than or equal to %d"),
    //
    SIZE_GREATER_THAN("size must be greater than %d"),
    SIZE_GREATER_THAN_OR_EQUAL("size must be greater than or equal to %d"),
    //
    SIZE_BETWEEN_INCLUSIVE("size must be between %d and %d (inclusive)"),

    /**
     * For numbers only, but the placeholder is {@code %s} to accept both integers and doubles.
     */
    NUM_GREATER_THAN("must be greater than %s"),
    NUM_GREATER_THAN_OR_EQUAL("must be greater than or equal to %s"),
    //
    NUM_LESS_THAN("must be less than %s"),
    NUM_LESS_THAN_OR_EQUAL("must be less than or equal to %s"),
    //
    NUM_BETWEEN_INCLUSIVE("must be between %s and %s (inclusive)"),
    NUM_EQUAL("must equal %s"),
    //
    NUM_POSITIVE("must be greater than 0"),
    NUM_POSITIVE_OR_ZERO("must be greater than or equal to 0"),

    /**
     * For temporal values.
     */
    BEFORE("must be before %s"),
    BEFORE_OR_EQUAL("must be before or equal to %s"),
    //
    AFTER("must be after %s"),
    AFTER_OR_EQUAL("must be after or equal to %s");

    public static final String RAW_NAME_PLACEHOLDER = "%s";
    public static final String DECORATED_NAME_PLACEHOLDER = RAW_NAME_PLACEHOLDER;

    private final String template;

    ValidationMessage(String template) {
        this.template = DECORATED_NAME_PLACEHOLDER + " " + template;
    }

    /**
     * Formats the message with the given arguments. The first argument is always the field name being validated.
     *
     * @param args optional arguments for the message template (if the specific template needs them)
     * @return the formatted message
     */
    public String format(Object... args) {
        return String.format(Locale.US, template, args);
    }

}
