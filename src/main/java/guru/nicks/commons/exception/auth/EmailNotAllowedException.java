package guru.nicks.commons.exception.auth;

import guru.nicks.commons.exception.http.ForbiddenException;

import lombok.experimental.StandardException;

import java.util.Map;

/**
 * Thrown if email address (such as retrieved from JWT) doesn't match the list of allowed patterns.
 */
@SuppressWarnings("java:S110") // allow more than 5 parents
@StandardException
public class EmailNotAllowedException extends ForbiddenException {

    public EmailNotAllowedException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
