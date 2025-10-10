package guru.nicks.exception.auth;

import guru.nicks.exception.http.ForbiddenException;

import lombok.experimental.StandardException;

/**
 * Thrown if email address (such as retrieved from JWT) doesn't match the list of allowed patterns.
 */
@SuppressWarnings("java:S110") // allow more than 5 parents
@StandardException
public class EmailNotAllowedException extends ForbiddenException {
}
