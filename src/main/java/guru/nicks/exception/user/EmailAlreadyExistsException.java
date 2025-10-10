package guru.nicks.exception.user;

import guru.nicks.exception.http.ConflictException;

import lombok.experimental.StandardException;

/**
 * Thrown if email address supplied is not unique.
 */
@SuppressWarnings("java:S110") // allow more than 5 parents
@StandardException
public class EmailAlreadyExistsException extends ConflictException {
}
