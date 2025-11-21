package guru.nicks.commons.utils.text;

import guru.nicks.commons.utils.FutureUtils;
import guru.nicks.commons.validation.dsl.ValiDsl;

import am.ik.yavi.meta.ConstraintArguments;
import com.github.demidko.aot.WordformMeaning;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;

/**
 * Ngram-related utility methods. To search against ngrams, the search text should be split into n-grams too (usually
 * with {@link #createNgrams(String, Mode, NgramUtilsConfig)}). The more ngrams match against each other, the higher is
 * the search score.
 * <p>
 * Russian words are augmented with their singular 'lemmas' (morphologically analysed forms, not just stems), for
 * example: 'люди' ('humans') -&gt; 'человек' ('a human'). Why not just replace each word with its stem? Because
 * non-intuitive things may happen: a stem for 'Google' is 'googl' for some reason, therefore searching for 'gle'
 * wouldn't find 'Google'. Also, singulars may differ from plurals drastically - see example above.
 */
@UtilityClass
public class NgramUtils {

    /**
     * Creates all (prefix and non-prefix) ngrams for unique words.
     * <p>
     * WARNING: original words are part of their ngrams only if the word length is within the ngram length limits.
     *
     * @param str    input string
     * @param mode   mode of ngrams creation
     * @param config configuration
     * @return ngrams (modifiable collection, max. {@link NgramUtilsConfig#getMaxNgramCount()} elements to avoid memory
     *         overflow)
     */
    public static SequencedSet<String> createNgrams(String str, Mode mode, NgramUtilsConfig config) {
        SequencedSet<String> ngrams = switch (mode) {
            case ALL -> {
                SequencedSet<String> prefixNgrams = createPrefixNgrams(str, config);
                SequencedSet<String> infixNgrams = createInfixNgrams(str, config);
                // temporarily, this sets may hold two times the max. ngram count
                prefixNgrams.addAll(infixNgrams);
                yield prefixNgrams;
            }

            case PREFIX_ONLY -> createPrefixNgrams(str, config);
            case INFIX_ONLY -> createInfixNgrams(str, config);
        };

        @SuppressWarnings("java:S1488") // redundant local variable, for debugging
        var limitedNgrams = limitNgramCount(ngrams, config);
        return limitedNgrams;
    }

    /**
     * Creates prefix ngrams - processes each word starting with its 1st letter, for example: 'strings' -> 'str' 'stri',
     * 'strin', 'string' (if trigrams are needed, the only one is 'str').
     * <p>
     * WARNING: original words are part of the result only if the word length is within the ngram length limits.
     *
     * @param str    input string
     * @param config configuration
     * @return ngrams (modifiable collection, max. {@link NgramUtilsConfig#getMaxNgramCount()} elements to avoid memory
     *         overflow)
     */
    private static SequencedSet<String> createPrefixNgrams(String str, NgramUtilsConfig config) {
        return generateNgrams(str, config, 0, 0);
    }

    /**
     * Creates prefix ngrams - processes each word starting with its 2nd letter, for example trigrams: 'strings' ->
     * 'tri', 'rin', 'ing', 'ngs'.
     * <p>
     * WARNING: original words are part of the result only if the word length is within the ngram length limits.
     *
     * @param str    input string
     * @param config configuration
     * @return ngrams (modifiable collection, max. {@link NgramUtilsConfig#getMaxNgramCount()} elements to avoid memory
     *         overflow)
     */
    private static SequencedSet<String> createInfixNgrams(String str, NgramUtilsConfig config) {
        return generateNgrams(str, config, 1, Integer.MAX_VALUE);
    }

    /**
     * Creates ngrams for each unique word. Russian words are augmented with their singular 'lemmas' (morphologically
     * analysed forms, not just stems), for example: 'люди' ('humans') -&gt; 'человек' ('a human').
     * <p>
     * WARNING: original words are part of the result only if the word length is within the ngram length limits and
     * {@code startEachWordOffset} is 0.
     *
     * @param str                 input string
     * @param config              configuration
     * @param startEachWordOffset offset in each word to start at
     * @param endEachWordOffset   offset in each word to finish at (word lengths differ, so pass
     *                            {@link Integer#MAX_VALUE} to process each word fully)
     * @return ngrams (modifiable collection, max. {@link NgramUtilsConfig#getMaxNgramCount()} elements to avoid memory
     *         overflow)
     */
    private static SequencedSet<String> generateNgrams(String str, NgramUtilsConfig config,
            int startEachWordOffset, int endEachWordOffset) {
        // avoid processing the same word twice
        Set<String> words = TextUtils.collectUniqueWords(str, config.isReduceAccents());

        int maxNgramLength = (startEachWordOffset == 0)
                ? config.getMaxPrefixNgramLength()
                : config.getMaxInfixNgramLength();
        var ngrams = new LinkedHashSet<String>();

        // Process words in parallel according to settings. Don't pre-create a supplier for each word - the number
        // of words may be large. Instead, create suppliers in chunks.
        for (var iterator = words.iterator(); iterator.hasNext(); ) {
            var wordProcessors = new ArrayList<Supplier<Set<String>>>(config.getMaxThreads());

            for (int i = 0; (i < config.getMaxThreads()) && iterator.hasNext(); i++) {
                String word = iterator.next();

                wordProcessors.add(() -> {
                    SequencedSet<String> wordNgrams = generatePlainNgrams(word,
                            startEachWordOffset, endEachWordOffset,
                            config.getMinNgramLength(), maxNgramLength);

                    if (config.tryMorphAnalysis()) {
                        SequencedSet<String> morphNgrams = generateMorphNgrams(word,
                                startEachWordOffset, endEachWordOffset,
                                config.getMinNgramLength(), maxNgramLength);

                        wordNgrams.addAll(morphNgrams);
                    }

                    return wordNgrams;
                });
            }

            FutureUtils.getInParallel(wordProcessors)
                    .forEach(ngrams::addAll);
        }

        return limitNgramCount(ngrams, config);
    }

    /**
     * Given a string, generates ngrams for it.
     *
     * @param word           word to process
     * @param startOffset    offset in string to start at (if it's equal to or greater than the string length, nothing
     *                       is done)
     * @param endOffset      offset in string to finish at (if it's equal to or greater than the string length, the
     *                       input string is simply processed fully)
     * @param minNgramLength minimum ngram length
     * @param maxNgramLength maximum ngram length (will be normalized to fit in the string length)
     * @return ngrams (modifiable collection)
     */
    @ConstraintArguments
    private static SequencedSet<String> generatePlainNgrams(String word,
            int startOffset, int endOffset,
            int minNgramLength, int maxNgramLength) {
        ValiDsl.check(startOffset, _NgramUtilsGeneratePlainNgramsArgumentsMeta.STARTOFFSET.name()).positiveOrZero();
        check(endOffset, _NgramUtilsGeneratePlainNgramsArgumentsMeta.ENDOFFSET.name()).constraint(it ->
                it >= startOffset, "must be >= " + _NgramUtilsGeneratePlainNgramsArgumentsMeta.STARTOFFSET.name());

        check(minNgramLength, _NgramUtilsGeneratePlainNgramsArgumentsMeta.MINNGRAMLENGTH.name()).positive();
        check(maxNgramLength, _NgramUtilsGeneratePlainNgramsArgumentsMeta.MAXNGRAMLENGTH.name()).constraint(it ->
                        it >= minNgramLength,
                "must be >= " + _NgramUtilsGeneratePlainNgramsArgumentsMeta.MINNGRAMLENGTH.name());

        var ngrams = new LinkedHashSet<String>();

        // nothing to do if the string is too short
        if (StringUtils.isBlank(word) || (startOffset >= word.length())) {
            return ngrams;
        }

        int fixedMaxGramLength = Math.min(word.length() - startOffset, maxNgramLength);
        int fixedEndOffset = Math.min(word.length() - fixedMaxGramLength, endOffset);

        for (int i = startOffset; i <= fixedEndOffset; i++) {
            for (int ngramLength = minNgramLength; ngramLength <= fixedMaxGramLength; ngramLength++) {
                if (i + ngramLength > word.length()) {
                    break;
                }

                ngrams.add(word.substring(i, i + ngramLength));
            }
        }

        return ngrams;
    }

    /**
     * Performs morphology analysis for Russian and creates ngrams for the 'lemma' (kind of word stem, but smarter). For
     * arguments and return value, see {@link #generatePlainNgrams(String, int, int, int, int)}.
     *
     * @return ngrams (modifiable collection)
     */
    private static SequencedSet<String> generateMorphNgrams(String word,
            int startEachWordOffset, int endEachWordOffset,
            int minNgramLength, int maxNgramLength) {
        var ngrams = new LinkedHashSet<String>();

        // returns empty list for unknown words, for example for all non-Russian ones
        for (WordformMeaning meaning : WordformMeaning.lookupForMeanings(word)) {
            String lemma = meaning.getLemma().toString();

            // lemma might be the word itself or a prefix of the original word (i.e. part of prefix ngrams)
            if (!Strings.CS.startsWith(word, lemma)) {
                Set<String> plainNgrams = generatePlainNgrams(lemma,
                        startEachWordOffset, endEachWordOffset,
                        minNgramLength, maxNgramLength);
                ngrams.addAll(plainNgrams);
            }
        }

        return ngrams;
    }

    /**
     * Limits the number of ngrams.
     *
     * @param ngrams ngrams to process
     * @param config configuration
     * @return original or truncated ngrams
     */
    private static SequencedSet<String> limitNgramCount(SequencedSet<String> ngrams, NgramUtilsConfig config) {
        if (ngrams.size() <= config.getMaxNgramCount()) {
            return ngrams;
        }

        return ngrams.stream()
                .limit(config.getMaxNgramCount())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public enum Mode {

        /**
         * Create both prefix and infix ngrams.
         */
        ALL,

        /**
         * Create prefix (i.e. those starting with the 1st character) ngrams only.
         */
        PREFIX_ONLY,

        /**
         * Create infix (i.e. those starting with the 2nd character) ngrams only.
         */
        INFIX_ONLY

    }

}
