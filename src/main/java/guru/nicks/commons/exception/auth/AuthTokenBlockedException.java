package guru.nicks.commons.exception.auth;

import guru.nicks.commons.exception.http.UnauthorizedException;

import lombok.experimental.StandardException;

import java.util.Map;

/**
 * Thrown if JWT has been blocked explicitly, for example due to user sign-out.
 */
@SuppressWarnings("java:S110") // allow more than 5 parents
@StandardException
public class AuthTokenBlockedException extends UnauthorizedException {

    public AuthTokenBlockedException(Map<String, Object> additionalResponseHeaders) {
        super(additionalResponseHeaders);
    }

}
