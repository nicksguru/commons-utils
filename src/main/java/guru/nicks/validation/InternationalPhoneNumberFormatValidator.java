package guru.nicks.validation;

import guru.nicks.utils.PhoneNumberUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates fields annotated with {@link InternationalPhoneNumberFormat @InternationalPhoneNumberFormat}.
 * <p>
 * NOTE: {@code null} values are considered valid, empty and all-whitespaces ones are not.
 */
public class InternationalPhoneNumberFormatValidator
        implements ConstraintValidator<InternationalPhoneNumberFormat, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) {
            return true;
        }

        return PhoneNumberUtils.isValidInternationalPhoneNumber(value);
    }

}
