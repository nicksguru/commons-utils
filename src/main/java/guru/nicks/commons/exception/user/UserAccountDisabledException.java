package guru.nicks.commons.exception.user;

import guru.nicks.commons.exception.http.ForbiddenException;

import lombok.experimental.StandardException;

import java.util.Map;

/**
 * Thrown if user exists, but is disabled/deactivated/suspended.
 */
@SuppressWarnings("java:S110") // allow more than 5 parents
@StandardException
public class UserAccountDisabledException extends ForbiddenException {

    public UserAccountDisabledException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
