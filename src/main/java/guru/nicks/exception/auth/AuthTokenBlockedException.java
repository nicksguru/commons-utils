package guru.nicks.exception.auth;

import guru.nicks.exception.http.UnauthorizedException;

import lombok.experimental.StandardException;

/**
 * Thrown if JWT has been blocked explicitly, for example due to user sign-out.
 */
@SuppressWarnings("java:S110") // allow more than 5 parents
@StandardException
public class AuthTokenBlockedException extends UnauthorizedException {
}
