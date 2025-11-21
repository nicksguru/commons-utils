package guru.nicks.commons.validation;

import guru.nicks.commons.utils.text.PhoneNumberUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates fields annotated with {@link InternationalPhoneNumberFormat @InternationalPhoneNumberFormat}.
 * <p>
 * NOTE: {@code null} values are considered valid; empty and all-whitespace ones are not.
 */
public class InternationalPhoneNumberFormatValidator
        implements ConstraintValidator<InternationalPhoneNumberFormat, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        return (value == null)
                || PhoneNumberUtils.isValidInternationalPhoneNumber(value);
    }

}
