package guru.nicks.exception.http;

import guru.nicks.exception.BusinessException;
import guru.nicks.exception.RootHttpStatus;

import lombok.experimental.StandardException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * HTTP status 429. All exceptions having the same HTTP status must inherit from this one.
 */
@RootHttpStatus(HttpStatus.TOO_MANY_REQUESTS)
@StandardException
public class RateLimitExceededException extends BusinessException {

    /**
     * One of HTTP headers often accompanying this exception is {@link HttpHeaders#RETRY_AFTER} whose value is the
     * number of seconds to wait before retrying the request.
     */
    public RateLimitExceededException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
