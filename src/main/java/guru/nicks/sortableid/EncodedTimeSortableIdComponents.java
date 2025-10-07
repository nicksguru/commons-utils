package guru.nicks.sortableid;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import lombok.extern.jackson.Jacksonized;

/**
 * {@link TimeSortableIdComponents} after encoding.
 */
@Value
@NonFinal
@Jacksonized
@Builder(toBuilder = true)
@FieldNameConstants
public class EncodedTimeSortableIdComponents {

    String encodedTimestamp;
    String encodedSequence;

}
