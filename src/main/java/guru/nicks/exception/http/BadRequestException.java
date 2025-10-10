package guru.nicks.exception.http;

import guru.nicks.exception.BusinessException;
import guru.nicks.exception.RootHttpStatus;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * HTTP status 400. All exceptions having the same HTTP status must inherit from this one.
 */
@RootHttpStatus(HttpStatus.BAD_REQUEST)
@StandardException
public class BadRequestException extends BusinessException {

    public BadRequestException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
