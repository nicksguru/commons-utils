package guru.nicks.validation;

import guru.nicks.utils.ReflectionUtils;

import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.function.Predicate.not;

/**
 * Annotation-based object validator.
 */
@Component
@RequiredArgsConstructor
public class AnnotationValidator {

    // DI
    private final LocalValidatorFactoryBean validator;

    /**
     * Converts 'full.package.SomeClassName' to 'someClassName' which, when used in {@link BindingResult}, becomes part
     * of the error messages, such as <code>NotBlank.className.fieldName</code>.
     *
     * @param obj object
     * @return class name for e.g. binding error messages
     */
    public String getClassNameForBindingResult(Object obj) {
        return StringUtils.uncapitalize(obj.getClass().getSimpleName());
    }

    /**
     * Validates object the same way as Spring's {@link Valid @Valid} does, with two differences:
     * <ol>
     *   <li>Nested property names don't have smart names like {@code products[0].price}, they're always
     *       plain ones ({@code price}).</li>
     *  <li>Spring does not validate complex properties (and collections of complex properties) unless they're
     *      annotated with {@link Valid @Valid}, but this method does. This is a safeguard against a missing
     *      {@link Valid @Valid} in some DTO which otherwise would have left invalid values unnoticed.</li>
     * </ol>
     *
     * @param obj object to validate
     * @throws ValidationException object is invalid (as per {@link BindException} which is
     *                             {@link ValidationException#getCause()})
     */
    public void validate(Object obj) {
        // avoid circular references - keep track of already visited objects
        var seen = new HashSet<>();
        validate(obj, seen);
    }

    private void validate(@Nullable Object obj, Set<Object> seen) {
        if ((obj == null) || seen.contains(obj)) {
            return;
        }

        BindingResult bindingResult = validateAndGetBindingResult(obj);
        seen.add(obj);

        if (bindingResult.hasErrors()) {
            throw new ValidationException(new BindException(bindingResult));
        }

        // validate all nested complex properties, including collections
        Arrays.stream(PropertyUtils.getPropertyDescriptors(obj.getClass()))
                .map(PropertyDescriptor::getReadMethod)
                .map(readMethod -> {
                    try {
                        return readMethod.invoke(obj);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new IllegalStateException("Failed to read property: " + e.getMessage(), e);
                    }
                })
                .filter(not(ReflectionUtils::isScalar))
                .filter(not(seen::contains))
                .forEach(nestedObj -> validate(nestedObj, seen));
    }

    private BindingResult validateAndGetBindingResult(Object obj) {
        // the 2nd parameter in fact defines the error message code suffix (after 'ErrorCode.')
        var bindingResult = new BeanPropertyBindingResult(obj, getClassNameForBindingResult(obj));
        validator.validate(obj, bindingResult);
        return bindingResult;
    }

}
