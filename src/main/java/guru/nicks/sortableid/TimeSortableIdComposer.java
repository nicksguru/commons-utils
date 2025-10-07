package guru.nicks.sortableid;

import guru.nicks.encoder.Encoder;

import am.ik.yavi.meta.ConstraintArguments;

import static guru.nicks.validation.dsl.ValiDsl.check;
import static guru.nicks.validation.dsl.ValiDsl.checkNotBlank;
import static guru.nicks.validation.dsl.ValiDsl.checkNotNull;

/**
 * Composes ID out of its encoded components (checksum is out of scope because it's already part of
 * {@link EncodedTimeSortableIdComponents#getEncodedSequence()}). Decomposes ID into its encoded components.
 */
public class TimeSortableIdComposer implements Encoder<EncodedTimeSortableIdComponents> {

    private final int timestampLength;

    @ConstraintArguments
    public TimeSortableIdComposer(int timestampLength) {
        check(timestampLength, _TimeSortableIdComposerArgumentsMeta.TIMESTAMPLENGTH.name()).positive();
        this.timestampLength = timestampLength;
    }

    @ConstraintArguments
    @Override
    public String encode(EncodedTimeSortableIdComponents idComponents) {
        checkNotNull(idComponents, _TimeSortableIdComposerEncodeArgumentsMeta.IDCOMPONENTS.name());
        check(idComponents.getEncodedTimestamp(), EncodedTimeSortableIdComponents.Fields.encodedTimestamp)
                .notBlank()
                .shorterThanOrEqual(timestampLength);
        checkNotBlank(idComponents.getEncodedSequence(), EncodedTimeSortableIdComponents.Fields.encodedSequence);

        return idComponents.getEncodedTimestamp() + idComponents.getEncodedSequence();
    }

    @ConstraintArguments
    @Override
    public EncodedTimeSortableIdComponents decode(String id) {
        check(id, _TimeSortableIdComposerDecodeArgumentsMeta.ID.name())
                .notBlank()
                .longerThan(timestampLength);

        String encodedTimestamp = id.substring(0, timestampLength);
        String encodedSequence = id.substring(timestampLength);

        return EncodedTimeSortableIdComponents.builder()
                .encodedTimestamp(encodedTimestamp)
                .encodedSequence(encodedSequence)
                .build();
    }

}
