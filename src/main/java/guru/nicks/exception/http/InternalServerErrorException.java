package guru.nicks.exception.http;

import guru.nicks.exception.BusinessException;
import guru.nicks.exception.RootHttpStatus;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * HTTP status 500. All exceptions having the same HTTP status must inherit from this one.
 */
@RootHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
@StandardException
public class InternalServerErrorException extends BusinessException {

    public InternalServerErrorException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
