package guru.nicks.commons.encoder;

import am.ik.yavi.meta.ConstraintArguments;
import com.google.common.primitives.Longs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Base10-to-BaseN <i>numeric</i> conversion, not a bitwise ('take {@link Longs#toByteArray(long)} and encode the
 * bytes') one. Therefore, any alphabet will do; to retain sort order, it must be pre-sorted. Throws exception on
 * negative/null numbers.
 */
@Slf4j
public class BaseNSequenceEncoder implements PaddingEncoder<Long> {

    private final int radix;
    private final String encodedZero;

    /**
     * Keys are numbers from 0 to N, values are their BaseN representations.
     */
    private final Map<Long, Character> encodeTable;

    /**
     * Keys are BaseN characters, values are their decimal equivalents - numbers from 0 to N.
     */
    private final Map<Character, Long> decodeTable;

    /**
     * Length of {@link Long#MAX_VALUE} after encoding.
     */
    private final int maxEncodedLength;

    /**
     * Constructor.
     *
     * @param alphabet must contain 2..{@link Integer#MAX_VALUE} characters, no whitespaces or duplicates
     */
    @ConstraintArguments
    public BaseNSequenceEncoder(String alphabet) {
        check(alphabet, _BaseNSequenceEncoderArgumentsMeta.ALPHABET.name())
                .notBlank()
                .lengthBetweenInclusive(2, Integer.MAX_VALUE)
                .constraint(str -> StringUtils.deleteWhitespace(str).equals(str), "has whitespaces")
                .constraint(str -> str.chars().distinct().count() == str.length(), "has duplicate characters");

        var tmpEncodeTable = new HashMap<Long, Character>();
        // populate tables
        for (int i = 0; i < alphabet.length(); i++) {
            char chr = alphabet.charAt(i);
            tmpEncodeTable.put((long) i, chr);
        }

        encodeTable = Map.copyOf(tmpEncodeTable);
        decodeTable = Map.copyOf(MapUtils.invertMap(encodeTable));
        radix = alphabet.length();
        encodedZero = String.valueOf(alphabet.charAt(0));

        String maxEncodedLong = encode(Long.MAX_VALUE);
        maxEncodedLength = maxEncodedLong.length();

        log.debug("Radix: {} (alphabet: '{}'), max. Long encoded: '{}' ({} chars)", radix, alphabet,
                maxEncodedLong, maxEncodedLength);

    }

    @ConstraintArguments
    @Override
    public String encode(Long sequence) {
        check(sequence, _BaseNSequenceEncoderEncodeArgumentsMeta.SEQUENCE.name()).positiveOrZero();

        if (sequence == 0) {
            return encodedZero;
        }

        var chars = new LinkedList<Character>();

        // divide by radix until the remainder (quotient) becomes 0
        for (long remainder = sequence; remainder > 0; remainder = Math.floorDiv(remainder, radix)) {
            long decimalValue = remainder % radix;
            Character chr = encodeTable.get(decimalValue);
            checkNotNull(chr, "Base" + radix + " character for decimal " + decimalValue);
            chars.addFirst(chr);
        }

        var builder = new StringBuilder();
        chars.forEach(builder::append);
        return builder.toString();
    }

    @ConstraintArguments
    @Override
    public Long decode(String value) {
        check(value, _BaseNSequenceEncoderDecodeArgumentsMeta.VALUE.name())
                .notBlank()
                .shorterThanOrEqual(maxEncodedLength);

        String normalized = StringUtils.stripStart(value, encodedZero);
        // to make loop below faster, compress leading zeroes to a single one
        if (normalized.isEmpty()) {
            normalized = encodedZero;
        }

        long number = 0;
        long factor = 1L;

        for (int i = normalized.length() - 1; i >= 0; i--, factor *= radix) {
            char chr = normalized.charAt(i);
            Long decimalValue = decodeTable.get(chr);

            // invalid character in input string
            if (decimalValue == null) {
                throw new IllegalArgumentException();
            }

            number += decimalValue * factor;
        }

        check(number, "decoded number").positiveOrZero();
        return number;
    }

    @Override
    public String padEncoded(String encoded, int padToLength) {
        return StringUtils.leftPad(encoded, padToLength, encodedZero);
    }

}
