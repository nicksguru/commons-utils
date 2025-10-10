package guru.nicks.exception;

import org.springframework.http.HttpStatus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies HTTP status for the exception class explicitly, thus also registering it for the reverse
 * (status-to-exception) mapping. Non-annotated exceptions inherit HTTP statuses from their annotated parents.
 * <p>
 * NOTE: only explicitly annotated exception classes are registered. Indirect annotations (i.e. found via inheritance)
 * are intentionally ignored. Therefore this annotation has no {@link java.lang.annotation.Inherited @Inherited}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface RootHttpStatus {

    HttpStatus value();

}
