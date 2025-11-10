package guru.nicks.service;

/**
 * Blocking a JWT means storing its checksum (with {@link #blockJwt(String)}) in a deny-list and then, in each HTTP
 * request, checking it with {@link #isJwtBlocked(String)}). Storing a crypto-grade checksum is crucial for security and
 * collision resistance.
 * <p>
 * The deny-list must be a singleton used by all microservices. For a Redis-based implementation, see Redis Starter.
 */
public interface BlockedJwtService {

    /**
     * Revokes the token i.e. puts it in the deny-list. The token remains there until it expires (which requires parsing
     * the JWT, but not validating its checksum/expiration).
     *
     * @param jwtAsString access token (JWT)
     * @throws IllegalArgumentException if the argument is blank or not a JWT
     */
    void blockJwt(String jwtAsString);

    /**
     * Checks if the token is blocked i.e. exists in the deny-list.
     *
     * @param jwtAsString token value (as passed in auth headers)
     * @return {@code true} if token is blocked
     * @throws IllegalArgumentException if the argument value is blank
     */
    boolean isJwtBlocked(String jwtAsString);

}
