package guru.nicks.exception.http;

import guru.nicks.exception.BusinessException;
import guru.nicks.exception.RootHttpStatus;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * HTTP status 405. All exceptions having the same HTTP status must inherit from this one.
 */
@RootHttpStatus(HttpStatus.METHOD_NOT_ALLOWED)
@StandardException
public class MethodNotAllowedException extends BusinessException {

    public MethodNotAllowedException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
