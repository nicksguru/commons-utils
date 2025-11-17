package guru.nicks.commons.encoder;

import guru.nicks.commons.utils.TimeUtils;

import am.ik.yavi.meta.ConstraintArguments;

import java.time.Duration;
import java.time.Instant;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Encodes timestamps as seconds augmented with approximate variable-precision subseconds, similar to UUIDv7.
 */
public class SubsecondTimestampEncoder implements Encoder<Instant> {

    private final PaddingEncoder<Long> longEncoder;
    private final int timestampLength;
    private final int decodeTimestampRoundSecondFraction;

    private final int subsecondBits;
    private final long subsecondFactor;
    private final long subsecondMask;

    /**
     * Constructor.
     *
     * @param longEncoder                encodes {@link Long} to {@link String} and vice versa
     * @param timestampLength            fixed (left-padded if needed) timestamp length after decoding (exception is
     *                                   thrown if it's exceeded)
     * @param subsecondBits              For fraction of second encoding using the following infinite progression:
     *                                   {@code 1 = 1/2 + 1/4 + 1/8 + ...} (0 means encode full seconds only).
     *                                   Empirically, the max. number of milliseconds lost is 1 if
     *                                   {@code subSecondBits >= 10}; {@code 10 - subSecondBits + 1} otherwise.
     * @param decodeSecondFractionDigits for rounding timestamps to this many floating point digits in
     *                                   {@link #decode(String)} to avoid 'floating number pollution' (the appearance of
     *                                   random subsecond digits)
     */
    @ConstraintArguments
    public SubsecondTimestampEncoder(PaddingEncoder<Long> longEncoder, int timestampLength, int subsecondBits,
            int decodeSecondFractionDigits) {
        this.longEncoder = checkNotNull(longEncoder, _SubsecondTimestampEncoderArgumentsMeta.LONGENCODER.name());

        this.timestampLength = check(timestampLength, _SubsecondTimestampEncoderArgumentsMeta.TIMESTAMPLENGTH.name())
                .positive()
                .getValue();

        this.subsecondBits = check(subsecondBits, _SubsecondTimestampEncoderArgumentsMeta.SUBSECONDBITS.name())
                .positiveOrZero()
                .getValue();
        subsecondFactor = 1L << subsecondBits;
        subsecondMask = subsecondFactor - 1;

        // fraction-of-second rounding only takes place is subseconds are allowed (9 digits are nanoseconds)
        if (subsecondBits > 0) {
            check(decodeSecondFractionDigits,
                    _SubsecondTimestampEncoderArgumentsMeta.DECODESECONDFRACTIONDIGITS.name())
                    .betweenInclusive(1, 9);
        }

        decodeTimestampRoundSecondFraction = decodeSecondFractionDigits;
    }

    @ConstraintArguments
    @Override
    public String encode(Instant timestamp) {
        checkNotNull(timestamp, _SubsecondTimestampEncoderEncodeArgumentsMeta.TIMESTAMP.name());
        Duration sinceCustomEpoch = TimeUtils.getDurationSinceCustomEpoch(timestamp);
        long number = sinceCustomEpoch.toSeconds();

        // add approximate subseconds (nanos include millis and possibly finer components too)
        if (subsecondBits > 0) {
            double fractionOfSecond = sinceCustomEpoch.toNanosPart() / 1_000_000_000.0;
            long subseconds = Math.round(fractionOfSecond * subsecondFactor);

            // Rounding above may produce, for example if 7 bits are allocated, 128 - more than 7 bits (127 is the
            // maximum). This means 1000 millis - seconds should be increased, and the number of millis becomes 0.
            if (subseconds == subsecondFactor) {
                number++;
                number <<= subsecondBits;
            } else {
                number <<= subsecondBits;
                number |= subseconds;
            }
        }

        String encodedTimestamp = longEncoder.encode(number);
        return longEncoder.padEncoded(encodedTimestamp, timestampLength);
    }

    @Override
    public Instant decode(String timestamp) {
        long number = longEncoder.decode(timestamp);
        long secondSinceCustomEpoch = number >> subsecondBits;
        long nanos = 0;

        if (subsecondBits > 0) {
            long subseconds = number & subsecondMask;
            double fractionOfSecond = subseconds / (double) subsecondFactor;

            // truncate fraction scale to avoid random polluting digits
            fractionOfSecond = TimeUtils.round(fractionOfSecond,
                    decodeTimestampRoundSecondFraction).doubleValue();
            nanos = Math.round(fractionOfSecond * 1_000_000_000.0);
        }

        return Instant.ofEpochSecond(
                TimeUtils.getCustomEpoch()
                        .plusSeconds(secondSinceCustomEpoch)
                        .getEpochSecond(), nanos);
    }

    @Override
    public boolean retainsSortOrderAfterEncoding() {
        return longEncoder.retainsSortOrderAfterEncoding();
    }

}
