package guru.nicks.commons.exception;

import guru.nicks.commons.exception.http.GoneException;

import lombok.experimental.StandardException;

import java.util.Map;

/**
 * A common use case of '410 Gone' when a resource is no longer available.
 */
@SuppressWarnings("java:S110") // allow more than 5 parents
@StandardException
public class ExpiredException extends GoneException {

    public ExpiredException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
