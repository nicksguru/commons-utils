package guru.nicks.commons.sortableid;

import am.ik.yavi.meta.ConstraintArguments;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Components embedded in time-sortable IDs.
 *
 * @see EncodedTimeSortableIdComponents
 */
@Value
@NonFinal
@Jacksonized
@Builder(toBuilder = true)
public class TimeSortableIdComponents {

    Instant timestamp;
    long sequence;

    /**
     * Constructor.
     *
     * @param timestamp timestamp, must not be {@code null}
     * @param sequence  sequence, must not be negative
     * @throws NullPointerException     {@code timestamp} is {@code null}
     * @throws IllegalArgumentException {@code sequence} is negative
     */
    @ConstraintArguments
    public TimeSortableIdComponents(Instant timestamp, long sequence) {
        this.timestamp = checkNotNull(timestamp, _TimeSortableIdComponentsArgumentsMeta.TIMESTAMP.name());

        this.sequence = check(sequence, _TimeSortableIdComponentsArgumentsMeta.SEQUENCE.name())
                .positiveOrZero()
                .getValue();
    }

}
