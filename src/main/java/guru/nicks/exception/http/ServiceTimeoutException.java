package guru.nicks.exception.http;

import guru.nicks.exception.BusinessException;
import guru.nicks.exception.RootHttpStatus;

import lombok.experimental.StandardException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * HTTP status 504 denotes connection timeout. All exceptions having the same HTTP status must inherit from this one.
 */
@RootHttpStatus(HttpStatus.GATEWAY_TIMEOUT)
@StandardException
public class ServiceTimeoutException extends BusinessException {

    public ServiceTimeoutException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
