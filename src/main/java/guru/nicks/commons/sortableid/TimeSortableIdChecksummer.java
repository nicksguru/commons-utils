package guru.nicks.commons.sortableid;

import guru.nicks.commons.encoder.Checksummer;
import guru.nicks.commons.encoder.Encoder;
import guru.nicks.commons.utils.ChecksumUtils;
import guru.nicks.commons.utils.HashUtils;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Computes and verifies checksum for {@link TimeSortableId#getId()}. Takes into account that the checksum is part of
 * the decimal sequence - its rightmost digit.
 */
@RequiredArgsConstructor
public class TimeSortableIdChecksummer implements Checksummer {

    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private final Encoder<EncodedTimeSortableIdComponents> idComposer;
    @NonNull // Lombok creates runtime nullness check for this own annotation only
    private final Encoder<Long> sequenceEncoder;

    /**
     * Computes checksum as a decimal digit.
     *
     * @param value arbitrary string
     * @return checksum (0..9)
     */
    @Override
    public String compute(String value) {
        char checksum = ChecksumUtils.computeExtendedCheckDigit(value, HashUtils.VERHOEFF::compute, "0123456789");
        return String.valueOf(checksum);
    }

    @Override
    public boolean isValid(String value) {
        EncodedTimeSortableIdComponents idComponents = idComposer.decode(value);
        long sequenceWithChecksum = sequenceEncoder.decode(idComponents.getEncodedSequence());
        String expectedChecksum = String.valueOf(sequenceWithChecksum % 10L);

        // re-encode sequence without checksum
        idComponents = idComponents.toBuilder()
                .encodedSequence(sequenceEncoder.encode(Math.floorDiv(sequenceWithChecksum, 10L)))
                .build();

        String actualChecksum = compute(idComposer.encode(idComponents));
        return actualChecksum.equals(expectedChecksum);
    }

}
