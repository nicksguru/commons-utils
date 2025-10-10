package guru.nicks.exception.http;

import guru.nicks.exception.BusinessException;
import guru.nicks.exception.RootHttpStatus;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * HTTP status 413. All exceptions having the same HTTP status must inherit from this one.
 */
@RootHttpStatus(HttpStatus.PAYLOAD_TOO_LARGE)
@StandardException
public class PayloadTooLargeException extends BusinessException {

    public PayloadTooLargeException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
