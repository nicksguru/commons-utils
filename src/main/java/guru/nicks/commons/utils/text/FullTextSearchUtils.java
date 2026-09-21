package guru.nicks.commons.utils.text;

import guru.nicks.commons.utils.crypto.ChecksumUtils;

import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Static helper holding the computational core for FTS: single-pass collection of the supplier text with its streaming
 * checksum + length-capped building of the ngram search data.
 */
@UtilityClass
public class FullTextSearchUtils {

    /**
     * Initial {@link StringBuilder} capacity for accumulating n-grams.
     */
    private static final int ESTIMATED_FTS_BUILDER_CAPACITY = 1024;

    /**
     * Estimated length of the source field for generating n-grams.
     */
    private static final int ESTIMATED_FTS_AWARE_FIELD_LENGTH = 50;

    /**
     * Maximum lexeme position accepted by the Postgres {@code tsvector} input syntax - positions beyond it are
     * rejected, so the weighted emission wraps the running counter back to 1.
     *
     * @see <a
     *         href="https://www.postgresql.org/docs/current/datatype-textsearch.html#DATATYPE-TSVECTOR">tsvector</a>
     */
    private static final int MAX_TSVECTOR_POSITION = 16383;

    /**
     * UTF-8 bytes of the single-space separator inserted between kept supplier values, cached to avoid re-encoding it
     * per value while streaming the checksum.
     */
    private static final byte[] FTS_VALUE_SEPARATOR_BYTES = " ".getBytes(StandardCharsets.UTF_8);

    /**
     * Collects full-text search data from the source suppliers in a single pass, appending each kept value to a builder
     * (needed only when the content has changed) while feeding an SHA-256 digest with exactly the bytes the joined text
     * would produce: UTF-8 bytes of each value plus a single-space separator between values. {@code null} suppliers and
     * blank values are skipped, an empty suppliers collection yields the digest of zero bytes.
     * <p>
     * The resulting checksum is therefore byte-identical to
     * {@link ChecksumUtils#computeJsonChecksum(Object) ChecksumUtils.computeJsonChecksum(joinedText)}, which avoids a
     * one-time rebuild of existing DB rows.
     *
     * @param suppliers search data suppliers, such as property getters; {@code null} suppliers and blank values are
     *                  ignored
     * @return collected search data: the accumulated builder plus the streaming checksum of the same content
     */
    public static FtsDataSource collectFtsDataSource(Collection<Supplier<String>> suppliers) {
        var digest = createEmptyDigest();

        // no suppliers - digest of zero bytes, matching the checksum of an empty text
        if (CollectionUtils.isEmpty(suppliers)) {
            return new FtsDataSource(new StringBuilder(0), encodeChecksum(digest));
        }

        // estimate initial capacity based on field count and average field length
        int estimatedCapacity = Math.max(
                ESTIMATED_FTS_BUILDER_CAPACITY,
                suppliers.size() * ESTIMATED_FTS_AWARE_FIELD_LENGTH);
        var sb = new StringBuilder(estimatedCapacity);

        // do not process each field individually - let the ngram creator detect unique words;
        // this is more memory-effective than 'Collectors.joining(" ")' for large texts
        for (Supplier<String> supplier : suppliers) {
            if (supplier == null) {
                continue;
            }

            String value = supplier.get();

            if (StringUtils.isBlank(value)) {
                continue;
            }

            // update BOTH the builder AND the digest
            if (!sb.isEmpty()) {
                sb.append(" ");
                digest.update(FTS_VALUE_SEPARATOR_BYTES);
            }

            // update BOTH the builder AND the digest
            sb.append(value);
            digest.update(value.getBytes(StandardCharsets.UTF_8));
        }

        // at this point, the builder is not materialized, but the digest is
        return new FtsDataSource(sb, encodeChecksum(digest));
    }

    /**
     * Validates the tokenized words, then appends short words and ngrams into a pre-sized builder, stopping at the
     * first chunk that would exceed the length cap.
     * <p>
     * With {@link NgramUtilsConfig#isWeightedTsvector()} on, emits the weighted tsvector input format instead: every
     * chunk is annotated as {@code 'chunk':positionWeight} with weight {@code A} for short words and prefix ngrams and
     * weight {@code B} for infix ngrams (see {@link #buildWeightedFtsData}).
     *
     * @param source    builder holding joined search text to create chunks of
     * @param config    ngram utils configuration
     * @param maxLength maximum length of the full-text search data; callers hoist it once per rebuild because it may be
     *                  a virtual call, and the config getters are interface default methods that may be computed in
     *                  subclasses
     * @return builder holding the length-capped chunk sequence, pre-sized to never exceed the length cap
     * @throws IllegalArgumentException search text contains characters suspicious of SQL injection (effectively
     *                                  unreachable because tokenization strips them - kept for behavioral
     *                                  compatibility)
     */
    public static String buildFtsData(StringBuilder source, NgramUtilsConfig config, int maxLength) {
        // Tokenize once - validation, the short-words phase and ngram creation below reuse the same word set.
        // This step also acts as a safeguard against SQL injection because it removes all punctuation.
        SortedSet<String> uniqueWords = TextUtils.collectUniqueWords(source.toString(), config.isReduceAccents());

        return config.isWeightedTsvector()
                ? buildWeightedFtsData(uniqueWords, config, maxLength)
                : buildPlainFtsData(uniqueWords, config, maxLength);
    }

    /**
     * Plain, unweighted emission: chunks separated by single spaces - the byte-identical behavior of the
     * pre-weighted-format implementation.
     *
     * @param uniqueWords tokenized unique words to create chunks of
     * @param config      ngram utils configuration
     * @param maxLength   maximum length of the full-text search data
     * @return length-capped space-separated chunk sequence
     */
    private static String buildPlainFtsData(Set<String> uniqueWords, NgramUtilsConfig config, int maxLength) {
        int minNgramLength = config.getMinNgramLength();
        int estimatedTotalLength = estimateFtsDataLength(config, minNgramLength, uniqueWords);

        // ensure the estimated capacity (a hint to StringBuilder, but not a limit) never exceeds the DB cap
        int estimatedCapacity = Math.clamp(
                Math.max(ESTIMATED_FTS_BUILDER_CAPACITY, estimatedTotalLength),
                0, maxLength);
        var builder = new StringBuilder(estimatedCapacity);

        // phase 1: short words (alphabetical, from the sorted word set) - appended only if shorter than the minimum
        // ngram length (otherwise they're already part of their ngrams) and not filtered out as English stop words
        boolean lengthCapReached = false;

        for (String word : uniqueWords) {
            if (word.length() >= minNgramLength) {
                continue;
            }

            // either English morph analysis is off or the word is not an English stop word (fast path - words are
            // already lowercase and trimmed)
            if (config.tryEnglishMorphAnalysis() && EnglishUtils.stopWord(word, true)) {
                continue;
            }

            if (!appendFtsChunk(builder, word, maxLength)) {
                lengthCapReached = true;
                break;
            }
        }

        // phase 2: ngrams in creation order; skipped entirely if the length cap already stopped the short-words phase
        if (!lengthCapReached) {
            for (String ngram : NgramUtils.createNgrams(uniqueWords, NgramUtils.Mode.ALL, config)) {
                if (!appendFtsChunk(builder, ngram, maxLength)) {
                    break;
                }
            }
        }

        return builder.toString();
    }

    /**
     * Weighted emission in the Postgres {@code tsvector} input syntax: every chunk is annotated with its 1-based
     * position (wrapped at {@value #MAX_TSVECTOR_POSITION}, the maximum Postgres accepts) and a weight letter -
     * {@code A} for short words and prefix ngrams, {@code B} for infix ngrams.
     * <p>
     * When Postgres casts the stored string to {@code tsvector}, the weights survive, and {@code ts_rank}'s default
     * weight array makes an A-match worth 2.5 times a B-match - so prefix matches rank above infix ones. Chunk
     * ordering, dedup and caps are identical to the plain format; only the annotation differs.
     * <p>
     * Chunks never contain quotes or punctuation (tokenization strips them), so uniform single-quoting of lexemes is
     * safe.
     *
     * @param uniqueWords tokenized unique words to create chunks of
     * @param config      ngram utils configuration
     * @param maxLength   maximum length of the annotated full-text search data
     * @return length-capped annotated chunk sequence, e.g. {@code 'gur':1A 'uru':2A 'nick':3B}
     */
    private static String buildWeightedFtsData(Set<String> uniqueWords, NgramUtilsConfig config, int maxLength) {
        int minNgramLength = config.getMinNgramLength();

        // the 'chunk':positionWeight annotation roughly doubles the flat size; the estimate is deliberately rough
        int estimatedTotalLength = 2 * estimateFtsDataLength(config, minNgramLength, uniqueWords);
        int estimatedCapacity = Math.clamp(
                Math.max(ESTIMATED_FTS_BUILDER_CAPACITY, estimatedTotalLength),
                0, maxLength);
        var builder = new StringBuilder(estimatedCapacity);

        // phase 1: short words - same filtering as the plain format, highest weight 'A' like prefix ngrams
        boolean lengthCapReached = false;
        int position = 0;

        for (String word : uniqueWords) {
            if (word.length() >= minNgramLength) {
                continue;
            }

            // either English morph analysis is off or the word is not an English stop word (fast path - words are
            // already lowercase and trimmed)
            if (config.tryEnglishMorphAnalysis() && EnglishUtils.stopWord(word, true)) {
                continue;
            }

            position = nextTsvectorPosition(position);
            if (!appendWeightedFtsChunk(builder, word, true, position, maxLength)) {
                lengthCapReached = true;
                break;
            }
        }

        // phase 2: weighted ngrams in creation order; skipped entirely if the length cap already stopped the
        // short-words phase
        if (!lengthCapReached) {
            for (NgramUtils.WeightedNgram weightedNgram : NgramUtils.createWeightedNgrams(uniqueWords, config)) {
                position = nextTsvectorPosition(position);

                if (!appendWeightedFtsChunk(builder, weightedNgram.ngram(), weightedNgram.highPriority(),
                        position, maxLength)) {
                    break;
                }
            }
        }

        return builder.toString();
    }

    /**
     * Needed to pre-size {@link StringBuilder} to min(cap, estimate): never allocate past the DB cap. The estimate is
     * deliberately rough because {@link StringBuilder} grows gracefully when needed.
     *
     * @param config         ngram config
     * @param minNgramLength minimum ngram length
     * @param words          text split into (preferably unique, or the estimate will soar) words
     * @return estimated total length of the full-text search data
     */
    public static int estimateFtsDataLength(NgramUtilsConfig config, int minNgramLength, Collection<String> words) {
        int maxPrefixNgramLength = config.getMaxPrefixNgramLength();
        int maxInfixNgramLength = config.getMaxInfixNgramLength();
        int maxNgramCount = config.getMaxNgramCount();

        int averageChunkLength = (minNgramLength + Math.max(maxPrefixNgramLength, maxInfixNgramLength)) / 2 + 1;
        int estimatedNgramCount = Math.min(words.size() * NgramUtils.ASSUMED_NGRAMS_PER_WORD, maxNgramCount);
        return (words.size() + estimatedNgramCount) * averageChunkLength;
    }

    /**
     * Splits text into chunks for FTS.
     *
     * @param text   source text
     * @param config ngram utils configuration
     * @return set of chunks to use for FTS:
     *         <ul>
     *             <li>original unique words shorter then {@link NgramUtilsConfig#getMinNgramLength()} - with accents
     *                 reduced (such as {@code ä → a}) if {@link NgramUtilsConfig#isReduceAccents()} is on and stop
     *                 words (such as 'the', 'a', 'was', 'I') removed if
     *                 {@link NgramUtilsConfig#tryEnglishMorphAnalysis()} is on</li>
     *             <li>ngrams created according to {@link NgramUtilsConfig}</li>
     *         </ul>
     */
    public static SequencedSet<String> createFtsChunks(String text, NgramUtilsConfig config) {
        // tokenize once - both the short-words phase and ngram creation below reuse the same word set
        // (this also removes punctuation and threfore the risk of SQL injection)
        SequencedSet<String> uniqueWords = TextUtils.collectUniqueWords(text, config.isReduceAccents());

        // add words that are shorter than the minimum ngram length, otherwise they'll be omitted
        SequencedSet<String> chunks = uniqueWords.stream()
                .filter(word -> word.length() < config.getMinNgramLength())
                // either English morph analysis is off or the word is not an English stop word (fast path - words
                // from collectUniqueWords are already lowercase and trimmed)
                .filter(word -> !config.tryEnglishMorphAnalysis() || !EnglishUtils.stopWord(word, true))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        chunks.addAll(NgramUtils.createNgrams(uniqueWords, NgramUtils.Mode.ALL, config));
        return chunks;
    }

    /**
     * Appends a single chunk using the following semantics: a single space before every chunk except the first, with
     * the separator counted as part of the chunk when checking the length cap.
     *
     * @param builder   builder accumulating the chunks
     * @param chunk     chunk to append
     * @param maxLength maximum length of the full-text search data
     * @return {@code true} if the chunk fit and was appended, {@code false} if it would exceed the length cap
     */
    private static boolean appendFtsChunk(StringBuilder builder, String chunk, int maxLength) {
        int separatorLength = builder.isEmpty() ? 0 : 1;

        // stop appending chunks as soon as the limit is reached (break, not skip)
        if (builder.length() + separatorLength + chunk.length() > maxLength) {
            return false;
        }

        if (!builder.isEmpty()) {
            builder.append(' ');
        }

        builder.append(chunk);
        return true;
    }

    /**
     * Appends a single annotated chunk ({@code 'chunk':positionWeight}) with the same break-on-first-mismatch length
     * cap semantics as {@link #appendFtsChunk}, the annotation counted as part of the chunk.
     *
     * @param builder      builder accumulating the annotated chunks
     * @param chunk        chunk to append
     * @param highPriority whether the chunk belongs to the high-priority (weight {@code A}) tier
     * @param position     1-based wrapped tsvector position, as produced by {@link #nextTsvectorPosition(int)}
     * @param maxLength    maximum length of the full-text search data
     * @return {@code true} if the chunk fit and was appended, {@code false} if it would exceed the length cap
     */
    private static boolean appendWeightedFtsChunk(StringBuilder builder, String chunk, boolean highPriority,
            int position, int maxLength) {
        int separatorLength = builder.isEmpty() ? 0 : 1;
        // quotes around the chunk, a colon, the position digits and the weight letter
        int annotatedLength = separatorLength + chunk.length() + 4 + countPositionDigits(position);

        // stop appending chunks as soon as the limit is reached (break, not skip)
        if (builder.length() + annotatedLength > maxLength) {
            return false;
        }

        if (!builder.isEmpty()) {
            builder.append(' ');
        }

        builder.append('\'')
                .append(chunk)
                .append("':")
                .append(position)
                .append(highPriority ? 'A' : 'B');
        return true;
    }

    /**
     * Wraps the running position counter into the range 1..{@value #MAX_TSVECTOR_POSITION} accepted by the Postgres
     * tsvector input syntax.
     *
     * @param position previous position (0 before the first chunk)
     * @return next position, wrapped back to 1 after {@value #MAX_TSVECTOR_POSITION}
     */
    private static int nextTsvectorPosition(int position) {
        return position % MAX_TSVECTOR_POSITION + 1;
    }

    /**
     * @param position tsvector position, never exceeding {@value #MAX_TSVECTOR_POSITION}
     * @return number of decimal digits of the position (at most 5, so no string allocation is needed for the check)
     */
    private static int countPositionDigits(int position) {
        if (position < 10) {
            return 1;
        }

        if (position < 100) {
            return 2;
        }

        if (position < 1000) {
            return 3;
        }

        return (position < 10000)
                ? 4
                : 5;
    }

    /**
     * Creates a fresh SHA-256 digest - the same algorithm {@link ChecksumUtils#computeJsonChecksum(Object)} uses.
     *
     * @return new digest instance (not thread-safe, never shared)
     * @throws IllegalStateException SHA-256 is unavailable (impossible on a compliant JVM)
     */
    private static MessageDigest createEmptyDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        }
        // every Java platform implementation is required to support SHA-256 - unreachable
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Completes the streaming checksum in exactly the format {@link ChecksumUtils#computeJsonChecksum(Object)}
     * produces: basic (non-MIME, non-url-safe) Base64 with padding and no line wrapping.
     *
     * @param digest digest fed with the content bytes
     * @return Base64-encoded checksum
     */
    private static String encodeChecksum(MessageDigest digest) {
        return Base64.getEncoder().encodeToString(digest.digest());
    }

    /**
     * Single-pass collection result: the accumulated builder of the joined search text plus the streaming checksum of
     * the exact bytes that text would produce. The builder is materialized (via {@code toString()}) only when the
     * content has changed, keeping the common unchanged-content path free of full-text copies.
     *
     * @param builder  joined search text (kept values separated by a single space, {@code null} suppliers and blank
     *                 values skipped)
     * @param checksum Base64 SHA-256 checksum of the joined text bytes, identical to
     *                 {@link ChecksumUtils#computeJsonChecksum(Object)} of the materialized text
     */
    public record FtsDataSource(

            StringBuilder builder,
            String checksum) {
    }

}
