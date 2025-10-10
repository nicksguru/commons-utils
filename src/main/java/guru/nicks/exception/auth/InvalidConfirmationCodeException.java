package guru.nicks.exception.auth;

import guru.nicks.exception.http.ConflictException;

import lombok.experimental.StandardException;

/**
 * Thrown if the confirmation code entered by user is invalid or not found.
 */
@SuppressWarnings("java:S110") // allow more than 5 parents
@StandardException
public class InvalidConfirmationCodeException extends ConflictException {
}
