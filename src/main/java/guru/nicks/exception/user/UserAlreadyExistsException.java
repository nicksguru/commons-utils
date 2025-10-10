package guru.nicks.exception.user;

import guru.nicks.exception.http.ConflictException;

import lombok.experimental.StandardException;

@SuppressWarnings("java:S110") // allow more than 5 parents
@StandardException
public class UserAlreadyExistsException extends ConflictException {
}
