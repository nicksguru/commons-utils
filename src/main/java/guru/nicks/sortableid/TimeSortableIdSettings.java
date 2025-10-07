package guru.nicks.sortableid;

import guru.nicks.encoder.Checksummer;
import guru.nicks.encoder.CrockfordBase32SequenceEncoder;
import guru.nicks.encoder.Encoder;
import guru.nicks.encoder.SubsecondTimestampEncoder;

import java.time.Instant;

/**
 * {@link #DEFAULT} settings let generate sortable IDs during 544 years since the custom Epoch. The ID length varies
 * from 10 to 22 characters (14 characters for sequence values ranging from 1_050_000 to 33_000_000; start DB sequences
 * with 1_050_000 to see 14 characters right away and during quite a time).
 */
public interface TimeSortableIdSettings {

    TimeSortableIdSettings DEFAULT = new TimeSortableIdSettings() {
    };

    /**
     * Not {@link Long#MAX_VALUE} because the rightmost decimal position is allotted to the checksum digit. This yields
     * a 10 times smaller value (minus 1).
     */
    default long getMaxEncodableSequence() {
        return Long.MAX_VALUE / 10 - 1;
    }

    /**
     * If 0, the timestamp has seconds only. Otherwise, this many fraction-of-second bits are appended. The more bits
     * are allocated, the finer is the precision. To encode 1000 milliseconds, 10 bits suffice ({@code 2 ** 10 = 1024}).
     * Likewise, 11 bits ({@code 2 ** 11 = 2048}) seem enough to encode 1/2 of a millisecond.
     */
    default int getSubsecondBits() {
        return 11;
    }

    /**
     * This many characters remain after trimming/padding the string encoded. Max. value expressible with this length
     * depends on the encoding format.
     */
    default int getPaddedTimestampLength() {
        return 9;
    }

    /**
     * Number of fraction digits to retain to avoid random polluting fraction digits. This field is only needed/used if
     * subseconds are allowed in the timestamp.
     *
     * @see #getSubsecondBits()
     */
    default int getDecodeTimestampRoundSecondFraction() {
        return 3;
    }

    /**
     * As the result of approximate subsecond precision, the delta between the original timestamp and the one after
     * encoding is up to this many millis.
     * <p>
     * Needed just to log a warning in the encoder which parses back the string encoded and compares it to the original
     * value.
     */
    default int getMaxEncodedTimestampDeltaMs() {
        return (getSubsecondBits() < 10)
                ? (10 - getSubsecondBits() + 1)
                : 1;
    }

    default Encoder<Instant> getTimestampEncoder() {
        return new SubsecondTimestampEncoder(CrockfordBase32SequenceEncoder.INSTANCE,
                getPaddedTimestampLength(), getSubsecondBits(), getDecodeTimestampRoundSecondFraction());
    }

    default Encoder<Long> getSequenceEncoder() {
        return CrockfordBase32SequenceEncoder.INSTANCE;
    }

    default Encoder<EncodedTimeSortableIdComponents> getIdComposer() {
        return new TimeSortableIdComposer(getPaddedTimestampLength());
    }

    default Checksummer getIdChecksummer() {
        return new TimeSortableIdChecksummer(getIdComposer(), getSequenceEncoder());
    }

}
