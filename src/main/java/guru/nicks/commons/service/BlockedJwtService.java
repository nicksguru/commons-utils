package guru.nicks.commons.service;

import guru.nicks.commons.exception.http.ForbiddenException;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.function.Function;

/**
 * Blocking a JWT means storing its checksum (with {@link #blockJwt(Jwt)}) in a deny-list and then, in each HTTP
 * request, checking it with {@link #isJwtBlocked(Jwt)}). Storing a crypto-grade checksum is crucial for security and
 * collision resistance.
 * <p>
 * The deny-list must be a singleton used by all microservices. For a Redis-based implementation, see Redis Starter.
 * <p>
 * Callers are expected to pass an already-parsed {@link Jwt} (e.g. produced by a Spring Security {@code JwtDecoder}),
 * so this service is not responsible for parsing or validating the token signature/structure.
 */
public interface BlockedJwtService {

    /**
     * Restricts access to user-owned tokens only.
     *
     * @param jwt    token (already parsed)
     * @param userId user ID (must equal the JWT {@code sub} claim)
     * @param mapper function to call for {@code jwt}
     * @param <T>    mapper result type
     * @return what the mapper returns
     * @throws IllegalArgumentException {@code jwt} is null
     * @throws ForbiddenException       token does not belong to the given user
     */
    <T> T ifBelongsToUser(Jwt jwt, String userId, Function<? super Jwt, T> mapper);

    /**
     * Revokes the token i.e. puts it in the deny-list. The token remains there until it expires.
     *
     * @param jwt token (already parsed)
     * @throws IllegalArgumentException {@code jwt} is null
     */
    void blockJwt(Jwt jwt);

    /**
     * Checks if the token is blocked i.e. exists in the deny-list.
     *
     * @param jwt token (already parsed)
     * @return {@code true} if token is blocked
     * @throws IllegalArgumentException {@code jwt} is null
     */
    boolean isJwtBlocked(Jwt jwt);

}
