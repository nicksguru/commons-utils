package guru.nicks.commons.exception.http;

import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.RootHttpStatus;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * HTTP status 409. All exceptions having the same HTTP status must inherit from this one.
 */
@RootHttpStatus(HttpStatus.CONFLICT)
@StandardException
public class ConflictException extends BusinessException {

    public ConflictException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
