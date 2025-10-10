package guru.nicks.exception.http;

import guru.nicks.exception.BusinessException;
import guru.nicks.exception.RootHttpStatus;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * HTTP status 501. All exceptions having the same HTTP status must inherit from this one.
 */
@RootHttpStatus(HttpStatus.NOT_IMPLEMENTED)
@StandardException
public class NotImplementedException extends BusinessException {

    public NotImplementedException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
