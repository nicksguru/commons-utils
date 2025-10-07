package guru.nicks.sortableid;

import am.ik.yavi.meta.ConstraintArguments;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Optional;

import static guru.nicks.validation.dsl.ValiDsl.check;

/**
 * For details, see {@link TimeSortableIdSettings}.
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class TimeSortableId extends TimeSortableIdComponents {

    private static final TimeSortableIdSettings CONFIG = TimeSortableIdSettings.DEFAULT;

    // ensure sort order is retained for both timestamps and sequences
    static {
        if (!CONFIG.getTimestampEncoder().retainsSortOrderAfterEncoding()) {
            throw new IllegalArgumentException(CONFIG.getTimestampEncoder().getClass().getName()
                    + " does not retain sort order");
        }

        if (!CONFIG.getSequenceEncoder().retainsSortOrderAfterEncoding()) {
            throw new IllegalArgumentException(CONFIG.getSequenceEncoder().getClass().getName()
                    + " does not retain sort order");
        }
    }

    private final String id;

    /**
     * Shortcut to {@link #TimeSortableId(Instant, long)}, passes {@link Instant#now()} as the timestamp. This is the
     * most typical use case; overriding the timestamp is mostly needed for testing.
     *
     * @param sequence value to encode/obfuscate
     */
    public TimeSortableId(long sequence) {
        this(Instant.now(), sequence);
    }

    /**
     * Performs encoding (sets {@link #getId()}).
     *
     * @param timestamp timestamp to encode
     * @param sequence  sequence to encode
     */
    @ConstraintArguments
    public TimeSortableId(Instant timestamp, long sequence) {
        super(timestamp, sequence);
        check(sequence, _TimeSortableIdArgumentsMeta.SEQUENCE.name())
                .betweenInclusive(0L, CONFIG.getMaxEncodableSequence());

        var idComponents = EncodedTimeSortableIdComponents.builder()
                .encodedTimestamp(CONFIG.getTimestampEncoder().encode(timestamp))
                .encodedSequence(CONFIG.getSequenceEncoder().encode(sequence))
                .build();

        // don't pass raw values because a timestamp decoded is not the same as its original value (due to rounding)
        String checksum = CONFIG.getIdChecksummer().compute(CONFIG.getIdComposer().encode(idComponents));
        int checkDigit = Integer.parseInt(checksum);
        check(checkDigit, "check digit").betweenInclusive(0, 9);

        // now with checksum added to sequence
        idComponents = idComponents.toBuilder()
                .encodedSequence(CONFIG.getSequenceEncoder().encode(sequence * 10L + checkDigit))
                .build();
        id = CONFIG.getIdComposer().encode(idComponents);

        TimeSortableIdComponents decoded = decode(id).orElseThrow(() ->
                new IllegalStateException("ID just generated can't be decoded"));
        validateGeneratedId(this, decoded);
    }

    /**
     * Called from {@link #decode(String)} to store the values. Non-public method because ID consistency isn't checked
     * against the timestamp and the sequence.
     *
     * @param id        ID
     * @param timestamp timestamp
     * @param sequence  sequence
     */
    protected TimeSortableId(String id, Instant timestamp, long sequence) {
        super(timestamp, sequence);
        this.id = id;
    }

    /**
     * Decodes the given ID.
     *
     * @param id ID to decode
     * @return {@link Optional#empty()} on decoding failure
     */
    public static Optional<TimeSortableId> decode(String id) {
        try {
            if (!CONFIG.getIdChecksummer().isValid(id)) {
                throw new IllegalArgumentException("Invalid checksum");
            }

            EncodedTimeSortableIdComponents idComponents = CONFIG.getIdComposer().decode(id);
            Instant timestamp = CONFIG.getTimestampEncoder().decode(idComponents.getEncodedTimestamp());

            // rightmost decimal position is allotted to the checksum digit
            long sequence = CONFIG.getSequenceEncoder().decode(idComponents.getEncodedSequence());
            sequence = Math.floorDiv(sequence, 10L);

            return Optional.of(new TimeSortableId(id, timestamp, sequence));
        } catch (RuntimeException e) {
            log.debug("ID can't be decoded: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Combines {@link #decode(String)} and {@link Optional#isPresent()}.
     *
     * @param id ID to validate
     * @return {@code true} on decoding success
     */
    public static boolean isValid(String id) {
        return decode(id).isPresent();
    }

    /**
     * Compares values extracted out of the ID to the original ones. Needed to ensure the values encoded can be decoded
     * correctly. If the timestamps differ more than {@link TimeSortableIdSettings#getMaxEncodedTimestampDeltaMs}, an
     * error message is logged, but no exception is thrown - because the 'too much' cannot be defined strictly.
     *
     * @param before values before encoding
     * @param after  values after encoding
     * @throws IllegalStateException sequence decoded is not equal to the original one
     */
    protected void validateGeneratedId(TimeSortableIdComponents before, TimeSortableIdComponents after) {
        if (before.getSequence() != after.getSequence()) {
            throw new IllegalStateException("ID generated decodes incorrectly (sequence)");
        }

        if (before.getTimestamp().getNano() == after.getTimestamp().getNano()) {
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace("Subseconds altered after encoding: (limit {}ms): {} -> {}",
                    CONFIG.getMaxEncodedTimestampDeltaMs(), before.getTimestamp(), after.getTimestamp());
        }

        // log insufficient timestamp precision
        if (Math.abs(before.getTimestamp().toEpochMilli() - after.getTimestamp().toEpochMilli())
                > CONFIG.getMaxEncodedTimestampDeltaMs()) {
            log.error("ID generated decodes incorrectly (timestamp): {} -> {} - delta exceeds {}ms",
                    before.getTimestamp(), after.getTimestamp(), CONFIG.getMaxEncodedTimestampDeltaMs());
        }
    }

}
