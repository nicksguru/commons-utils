package guru.nicks.commons.encoder;

/**
 * Encodes/decodes values.
 *
 * @param <T> input value type
 */
public interface Encoder<T> {

    /**
     * Encodes the given value. Throws exception on encoding failure.
     *
     * @param value value to encode
     * @return value encoded (has arbitrary length which depends on the concrete encoding)
     */
    String encode(T value);

    /**
     * Decodes the given value. Throws exception on decoding failure.
     *
     * @param value value to decode
     * @return value decoded
     * @throws IllegalArgumentException null / blank / too long / out-of-alphabet value
     */
    T decode(String value);

    /**
     * Returns {@code true} if sort order of strings after encoding is the same as that of the original numbers. For
     * example, Base58, Crockford's Base32, Base23hex do that; Base32 does not - it has digits after letters.
     *
     * @return default implementation returns {@code false}
     */
    default boolean retainsSortOrderAfterEncoding() {
        return false;
    }

}
