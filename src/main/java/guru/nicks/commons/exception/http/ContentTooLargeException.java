package guru.nicks.commons.exception.http;

import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.RootHttpStatus;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * HTTP status 413. All exceptions having the same HTTP status must inherit from this one.
 * <p>
 * This is 'content too large' in Spring Boot 4 and was 'payload too large' in Spring Boot 3.
 */
@RootHttpStatus(HttpStatus.CONTENT_TOO_LARGE)
@StandardException
public class ContentTooLargeException extends BusinessException {

    public ContentTooLargeException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
