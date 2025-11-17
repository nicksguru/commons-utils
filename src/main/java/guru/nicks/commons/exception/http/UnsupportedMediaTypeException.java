package guru.nicks.commons.exception.http;

import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.RootHttpStatus;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * HTTP status 415. All exceptions having the same HTTP status must inherit from this one.
 */
@RootHttpStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
@StandardException
public class UnsupportedMediaTypeException extends BusinessException {

    public UnsupportedMediaTypeException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
