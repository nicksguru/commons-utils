package guru.nicks.condition;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link Conditional @Conditional} that checks if the given property has a non-blank value. That is, not {@code null},
 * not an empty string, not whitespaces-only.
 */
@Conditional(OnPropertyNotBlankCondition.class)
@Target({
        // for @Component
        ElementType.TYPE,
        // for @Bean
        ElementType.METHOD
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConditionalOnPropertyNotBlank {

    /**
     * Property name.
     */
    String value();

}
