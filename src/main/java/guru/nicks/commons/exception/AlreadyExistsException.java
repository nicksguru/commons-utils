package guru.nicks.commons.exception;

import guru.nicks.commons.exception.http.ConflictException;

import lombok.experimental.StandardException;

import java.util.Map;

/**
 * A common use case of '409 Conflict' when a resource already exists.
 */
@SuppressWarnings("java:S110") // allow more than 5 parents
@StandardException
public class AlreadyExistsException extends ConflictException {

    public AlreadyExistsException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
