package guru.nicks.commons.encoder;

/**
 * Encodes/decodes values, also left-pads after encoding (not by default but provides a separate method for that).
 *
 * @param <T> input value type
 */
public interface PaddingEncoder<T> extends Encoder<T> {

    /**
     * Left-pads result of {@link #encode(Object)} with what serves as an encoded zero for the given encoding. Does
     * nothing if the input string is long enough already.
     *
     * @param encoded     string encoded
     * @param padToLength length to pad to
     * @return string padded if necessary
     */
    String padEncoded(String encoded, int padToLength);

}
