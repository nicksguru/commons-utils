package guru.nicks.utils;

import com.github.f4b6a3.uuid.UuidCreator;
import com.github.f4b6a3.uuid.codec.base.BaseN;
import com.github.f4b6a3.uuid.codec.base.BaseNCodec;
import com.github.f4b6a3.uuid.codec.base.function.Base32Decoder;
import com.github.f4b6a3.uuid.codec.base.function.Base32Encoder;
import com.github.f4b6a3.uuid.exception.InvalidUuidException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * UUID-related utility methods to be used instead of {@link UUID} methods.
 */
@UtilityClass
@Slf4j
public class UuidUtils {

    /**
     * UUIDv7's advantage is that not only it's time-sortable (by the milliseconds prefix) but also sortable within the
     * same milli thanks to an incrementing counter (whose initial value is random for each milli). This is also
     * beneficial for DB index locality: adjacent (by time) data is adjacent on disk.
     * <p>
     * WARNING: exposing the time of record creation may be a potential <b>security or business risk</b>. Shifting the
     * timestamp by a certain secret delta i.e. counting milliseconds since a 'custom Epoch' doesn't solve the problem -
     * anyone who can create, for example, a shop order can parse the UUID and see the difference between current time
     * and the one that the UUID claims. For such use cases, see {@link #generateUuidV4()}.
     * <table>
     *  <caption>Example from the
     *      <a href="https://github.com/f4b6a3/uuid-creator/wiki/1.7.-UUIDv7#type-1-default">documentation</a>
     *  </caption>
     *     <tr>
     *         <th>UUID</th>
     *         <th>Timestamp</th>
     *         <th>Version</th>
     *         <th>Counter</th>
     *     </tr>
     *     <tr>
     *          <td>0181be8a-e592-73d4-8504-09b220ad9b8c</td>
     *          <td>0181be8a-e59<b>2</b></td>
     *          <td>7</td>
     *          <td>3d4-850<b>4</b></td>
     *     </tr>
     *     <tr>
     *          <td>0181be8a-e592-73d4-8505-28cdc6fd9d64</td>
     *          <td>same as above</td>
     *          <td>7</td>
     *          <td>3d4-850<b>5</b> (incremented)</td>
     *     </tr>
     *     <tr>
     *          <td>0181be8a-e593-7693-8890-3fd3dc34aeec</td>
     *          <td>0181be8a-e59<b>3</b> (incremented)</td>
     *          <td>7</td>
     *          <td>693-8890 (randomized)</td>
     *     </tr>
     * </table>
     *
     * @return new UUID
     */
    public static UUID generateUuidV7() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    /**
     * Generates a random-based UUID v4 which suits such use cases where IDs exposed to the public must not reveal any
     * internal details, such as date/time of creation (like UUID v7 does).
     *
     * @return UUID v4
     */
    public static UUID generateUuidV4() {
        return UuidCreator.getRandomBased();
    }

    /**
     * Encodes UUID (of any version) with Crockford's Base32 flavor. Its advantage over plain Base32 is: if the original
     * data is sortable, its encoded representation is also sortable.
     *
     * @param uuid UUID
     * @return UUID encoded (26 alphanumeric characters)
     */
    public static String encodeToCrockfordBase32(UUID uuid) {
        return CrockfordBase32UuidCodec.INSTANCE.encode(uuid);
    }

    /**
     * Decodes a UUID from a Crockford Base32 encoded string.
     *
     * @param uuid the Crockford Base32 encoded UUID string
     * @return the decoded UUID
     * @throws InvalidUuidException if the input string is not a valid Crockford Base32 encoded UUID
     */
    public static UUID decodeFromCrockfordBase32(String uuid) {
        return CrockfordBase32UuidCodec.INSTANCE.decode(uuid);
    }

    /**
     * Does the same as {@link UUID#fromString(String)} but faster.
     *
     * @param uuid UUID as string
     * @return UUID parsed
     * @throws InvalidUuidException the argument is invalid
     */
    public static UUID parse(String uuid) {
        return UuidCreator.fromString(uuid);
    }

    private static class CrockfordBase32UuidCodec extends BaseNCodec {

        private static final BaseN BASE_N = new BaseN("0123456789abcdefghjkmnpqrstvwxyz");

        // it's crucial that this static var is created AFTER other static vars it depends on are initialized,
        // otherwise those vars will be null
        private static final CrockfordBase32UuidCodec INSTANCE = new CrockfordBase32UuidCodec();

        private CrockfordBase32UuidCodec() {
            super(BASE_N, new Base32Encoder(BASE_N), new Base32Decoder(BASE_N));
        }

    }

}
