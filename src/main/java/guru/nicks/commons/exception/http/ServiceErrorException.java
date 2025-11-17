package guru.nicks.commons.exception.http;

import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.RootHttpStatus;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * HTTP status 502 usually means remote party returned 5xx. All exceptions having the same HTTP status must inherit from
 * this one.
 */
@RootHttpStatus(HttpStatus.BAD_GATEWAY)
@StandardException
public class ServiceErrorException extends BusinessException {

    public ServiceErrorException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
