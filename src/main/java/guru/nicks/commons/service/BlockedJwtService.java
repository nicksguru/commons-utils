package guru.nicks.commons.service;

import guru.nicks.commons.exception.http.ForbiddenException;

import com.nimbusds.jwt.JWTClaimNames;

import java.util.function.Function;

/**
 * Blocking a JWT means storing its checksum (with {@link #blockJwt(String)}) in a deny-list and then, in each HTTP
 * request, checking it with {@link #isJwtBlocked(String)}). Storing a crypto-grade checksum is crucial for security and
 * collision resistance.
 * <p>
 * The deny-list must be a singleton used by all microservices. For a Redis-based implementation, see Redis Starter.
 */
public interface BlockedJwtService {

    /**
     * Restricts access to user-owned tokens only.
     *
     * @param jwtAsString token (JWT)
     * @param userId      user ID (must equal {@link JWTClaimNames#SUBJECT})
     * @param mapper      function to call for {@code jwtAsString}
     * @param <T>         mapper result type
     * @return what the mapper returns
     * @throws IllegalArgumentException if the argument is blank or not a JWT
     * @throws ForbiddenException       token  does not belong to the given user
     */
    <T> T ifBelongsToUser(String jwtAsString, String userId, Function<? super String, T> mapper);

    /**
     * Revokes the token i.e. puts it in the deny-list. The token remains there until it expires (which requires parsing
     * the JWT, but not validating its checksum/expiration).
     *
     * @param jwtAsString token (JWT)
     * @throws IllegalArgumentException if the argument is blank or not a JWT
     */
    void blockJwt(String jwtAsString);

    /**
     * Checks if the token is blocked i.e. exists in the deny-list.
     *
     * @param jwtAsString token (JWT)
     * @return {@code true} if token is blocked
     * @throws IllegalArgumentException if the argument value is blank
     */
    boolean isJwtBlocked(String jwtAsString);

}
