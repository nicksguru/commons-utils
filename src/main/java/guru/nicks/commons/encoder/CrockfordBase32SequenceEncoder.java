package guru.nicks.commons.encoder;

import guru.nicks.commons.utils.text.TextUtils;

/**
 * Based on Crockford's Base32 {@link TextUtils#CROCKFORD_BASE32_ALPHABET alphabet}.
 * <p>
 * Unlike the original spec which treats capital and lowercase letters equally and replaces 'i'/'l' with 1, 'o' with 0,
 * and 'u' with 'v', {@link #decode(String)} throws {@link IllegalArgumentException} in that case - for consistency and
 * validation purposes.
 */
public class CrockfordBase32SequenceEncoder extends BaseNSequenceEncoder {

    public static final CrockfordBase32SequenceEncoder INSTANCE = new CrockfordBase32SequenceEncoder();

    public CrockfordBase32SequenceEncoder() {
        super(TextUtils.CROCKFORD_BASE32_ALPHABET);
    }

    @Override
    public boolean retainsSortOrderAfterEncoding() {
        return true;
    }

}
