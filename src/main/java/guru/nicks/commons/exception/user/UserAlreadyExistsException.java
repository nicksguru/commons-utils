package guru.nicks.commons.exception.user;

import guru.nicks.commons.exception.AlreadyExistsException;

import lombok.experimental.StandardException;

import java.util.Map;

@SuppressWarnings("java:S110") // allow more than 5 parents
@StandardException
public class UserAlreadyExistsException extends AlreadyExistsException {

    public UserAlreadyExistsException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
