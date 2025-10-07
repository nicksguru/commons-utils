package guru.nicks.encoder;

/**
 * Computes and verifies checksum.
 */
public interface Checksummer {

    /**
     * Computes checksum.
     *
     * @param value arbitrary string
     * @return checksum
     */
    String compute(String value);

    /**
     * Verifies checksum.
     *
     * @param value arbitrary string
     * @return {@code true} if checksum is valid
     */
    boolean isValid(String value);

}
