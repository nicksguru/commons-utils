package guru.nicks.commons.exception.http;

import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.RootHttpStatus;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * HTTP status 403. All exceptions having the same HTTP status must inherit from this one.
 */
@RootHttpStatus(HttpStatus.FORBIDDEN)
@StandardException
public class ForbiddenException extends BusinessException {

    public ForbiddenException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
