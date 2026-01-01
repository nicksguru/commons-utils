package guru.nicks.commons.sortableid;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;

/**
 * {@link TimeSortableIdComponents} after encoding.
 */
@Value
@NonFinal
@Builder(toBuilder = true)
@FieldNameConstants
public class EncodedTimeSortableIdComponents {

    String encodedTimestamp;
    String encodedSequence;

}
