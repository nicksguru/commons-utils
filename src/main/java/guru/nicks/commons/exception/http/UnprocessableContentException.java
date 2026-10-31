package guru.nicks.commons.exception.http;

import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.RootHttpStatus;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * HTTP status 422. All exceptions having the same HTTP status must inherit from this one. This is 'unprocessable
 * content' in Spring Boot 4 and was 'unprocessable entity' in Spring Boot 3.
 */
@RootHttpStatus(HttpStatus.UNPROCESSABLE_CONTENT)
@StandardException
public class UnprocessableContentException extends BusinessException {

    public UnprocessableContentException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
