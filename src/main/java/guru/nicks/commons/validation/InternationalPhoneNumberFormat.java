package guru.nicks.commons.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.springframework.validation.FieldError;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates field for validation with {@link InternationalPhoneNumberFormatValidator}. The name of this annotation
 * matters: it goes to {@link FieldError#getCode()} which is then rendered to web app. Also, {@link #message()} is
 * rendered to REST caller.
 * <p>
 * NOTE: {@code null} values are considered valid, empty and all-whitespaces ones are not.
 */
@Constraint(validatedBy = InternationalPhoneNumberFormatValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InternationalPhoneNumberFormat {

    /**
     * Returned to caller in {@link FieldError#getDefaultMessage()}.
     *
     * @return error message
     */
    String message() default "Invalid international phone number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
