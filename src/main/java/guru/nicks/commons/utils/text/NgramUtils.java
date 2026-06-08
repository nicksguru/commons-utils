package guru.nicks.commons.utils.text;

import lombok.experimental.UtilityClass;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.Set;

/**
 * Ngram-related utility methods. To search against ngrams, the search text should be split into n-grams too (usually
 * with {@link #createNgrams(String, Mode, NgramUtilsConfig)}). The more ngrams match against each other, the higher is
 * the search score.
 * <p>
 * English words are, if {@link NgramUtilsConfig#tryEnglishMorphAnalysis()} is on, augmented with their singular
 * 'lemmas' (ran → run, geese → goose, etc.) with stop words ('the', 'a', 'be', etc.) filtered out because they would
 * match practically every DB record.
 * <p>
 * Russian words are, if {@link NgramUtilsConfig#tryRussianMorphAnalysis()} is on, augmented with their singular
 * 'lemmas' (morphologically analyzed forms, not just stems), for example: 'люди' ('humans') → 'человек' ('a human').
 * <p>
 * Why add lemmas to the original words, why not just replace each word with its lemma? Because the text to search in
 * may contain irregular words, see examples above.
 * <p>
 * <b>Optional dependency:</b> Morphological analysis requires the optional {@code com.github.demidko:aot} JAR
 * on the classpath. If not available, this feature is silently disabled and ngrams are generated without the Russian
 * morphological analysis. Include the dependency only when the Russian morphological analysis is needed.
 */
@UtilityClass
public class NgramUtils {

    public static final int ASSUMED_NGRAMS_PER_WORD = 7;

    /**
     * Creates ngrams for unique words.
     * <p>
     * WARNING: original words are part of their ngrams only if their length is within the ngram length limits. As such,
     * the word 'ox' is too short for trigrams.
     *
     * @param str    input string
     * @param mode   mode of ngrams creation
     * @param config configuration
     * @return ngrams (modifiable collection, max. {@link NgramUtilsConfig#getMaxNgramCount()} elements to avoid memory
     *         overflow, prefix ngrams go first - they have precedence before collection truncation)
     */
    public static SequencedSet<String> createNgrams(String str, Mode mode, NgramUtilsConfig config) {
        SequencedSet<String> ngrams = switch (mode) {
            case ALL -> {
                SequencedSet<String> prefixNgrams = createPrefixNgrams(str, config);
                SequencedSet<String> infixNgrams = createInfixNgrams(str, config);

                // temporarily, this set may hold two times the max. ngram count
                if (prefixNgrams.size() < config.getMaxNgramCount()) {
                    prefixNgrams.addAll(infixNgrams);
                }

                yield prefixNgrams;
            }

            case PREFIX -> createPrefixNgrams(str, config);
            case INFIX -> createInfixNgrams(str, config);
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
     * @return ngrams (modifiable collection, max. {@link NgramUtilsConfig#getMaxNgramCount()} items)
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
     * @return ngrams (modifiable collection, max. {@link NgramUtilsConfig#getMaxNgramCount()} items)
     */
    private static SequencedSet<String> createInfixNgrams(String str, NgramUtilsConfig config) {
        return generateNgrams(str, config, 1, Integer.MAX_VALUE);
    }

    /**
     * Creates ngrams for each unique word. Russian words are augmented with their singular 'lemmas' (morphologically
     * analyzed forms, not just stems), for example: 'люди' ('humans') → 'человек' ('a human').
     * <p>
     * WARNING: original words are part of the result only if the word length is within the ngram length limits and
     * {@code startEachWordOffset} is 0.
     *
     * @param str                 input string
     * @param config              configuration
     * @param startEachWordOffset offset in each word to start at
     * @param endEachWordOffset   offset in each word to finish at (word lengths differ, so pass
     *                            {@link Integer#MAX_VALUE} to process each word fully)
     * @return ngrams (modifiable collection, max. {@link NgramUtilsConfig#getMaxNgramCount()} items)
     */
    private static SequencedSet<String> generateNgrams(String str, NgramUtilsConfig config,
            int startEachWordOffset, int endEachWordOffset) {
        // avoid processing the same word twice
        Set<String> words = TextUtils.collectUniqueWords(str, config.isReduceAccents());
        SequencedSet<String> ngrams = LinkedHashSet.newLinkedHashSet(words.size() * ASSUMED_NGRAMS_PER_WORD);

        // 0 means prefix ngrams are to be generated
        int maxNgramLength = (startEachWordOffset == 0)
                ? config.getMaxPrefixNgramLength()
                : config.getMaxInfixNgramLength();

        for (String word : words) {
            addWordNgrams(config, startEachWordOffset, endEachWordOffset, word, maxNgramLength, ngrams);
        }

        return limitNgramCount(ngrams, config);
    }

    /**
     * Called from {@link #generateNgrams(String, NgramUtilsConfig, int, int)}. See description of arguments there.
     */
    private static void addWordNgrams(NgramUtilsConfig config, int startEachWordOffset, int endEachWordOffset,
            String word, int maxNgramLength, Set<String> whereToAdd) {
        // special case: English stop words don't make their way into ANY ngrams
        if (config.tryEnglishMorphAnalysis() && EnglishUtils.stopWord(word)) {
            return;
        }

        addRawNgrams(word, startEachWordOffset, endEachWordOffset,
                config.getMinNgramLength(), maxNgramLength, whereToAdd);

        if (config.tryEnglishMorphAnalysis()) {
            addEnglishMorphNgrams(word, startEachWordOffset, endEachWordOffset,
                    config.getMinNgramLength(), maxNgramLength, whereToAdd);
        }

        if (config.tryRussianMorphAnalysis()) {
            addRussianMorphNgrams(word, startEachWordOffset, endEachWordOffset,
                    config.getMinNgramLength(), maxNgramLength, whereToAdd);
        }
    }

    /**
     * Given a string, generates ngrams for it.
     * <p>
     * WARNING: the original word is part of the result only if its length is within the ngram length limits.
     *
     * @param word           word to process
     * @param startOffset    offset in string to start at (if it's equal to or greater than the string length, nothing
     *                       is done)
     * @param endOffset      offset in string to finish at (if it's equal to or greater than the string length, the
     *                       input string is simply processed fully)
     * @param minNgramLength minimum ngram length
     * @param maxNgramLength maximum ngram length (will be normalized to fit in the string length)
     * @param whereToAdd     where to add the ngrams
     */
    private static void addRawNgrams(String word, int startOffset, int endOffset,
            int minNgramLength, int maxNgramLength, Set<String> whereToAdd) {
        // nothing to do if the string is too short
        if (startOffset >= word.length()) {
            return;
        }

        int fixedMaxGramLength = Math.min(word.length() - startOffset, maxNgramLength);
        int fixedEndOffset = Math.min(word.length() - fixedMaxGramLength, endOffset);

        for (int i = startOffset; i <= fixedEndOffset; i++) {
            for (int ngramLength = minNgramLength; ngramLength <= fixedMaxGramLength; ngramLength++) {
                if (i + ngramLength > word.length()) {
                    break;
                }

                whereToAdd.add(word.substring(i, i + ngramLength));
            }
        }
    }

    /**
     * Performs morphology analysis for English and creates ngrams for the 'lemma' (kind of word stem, but smarter). For
     * arguments and return value, see {@link #addRawNgrams(String, int, int, int, int, Set)}. The common stop words
     * (such as 'the', be') are NOT filtered out, rather processed ('was' becomes 'be' etc.).
     * <p>
     * This method is light-weight, requires no dictionary, converts 'ran' to 'run', 'geese' to 'goose' and so on.
     * <p>
     * WARNING: the lemma is only processed if its length is within the ngram length and word offset limits. For
     * example, 'was' becomes 'be' whose length (2) is smaller than the common minimum ngram length (3). In this case,
     * this method returns an empty collection.s
     *
     * @param word must be non-blank and in lowercase already, for speed reasons
     */
    private static void addEnglishMorphNgrams(String word, int startEachWordOffset, int endEachWordOffset,
            int minNgramLength, int maxNgramLength, Set<String> whereToAdd) {
        String lemma = EnglishUtils.lemmatize(word);
        addRawNgrams(lemma, startEachWordOffset, endEachWordOffset,
                minNgramLength, maxNgramLength, whereToAdd);
    }

    /**
     * Performs morphology analysis for Russian and creates ngrams for the 'lemma' (kind of word stem, but smarter). For
     * arguments and return value, see {@link #addRawNgrams(String, int, int, int, int, Set)}.
     * <p>
     * This method only works if {@code com.github.demidko.aot.WordformMeaning} is available on the classpath. If the
     * class is not available, returns an empty collection.
     * <p>
     * WARNING: the lemma is only processed (split into ngrams) if its length is within the ngram length and word offset
     * limits.
     */
    private static void addRussianMorphNgrams(String word, int startEachWordOffset, int endEachWordOffset,
            int minNgramLength, int maxNgramLength, Set<String> whereToAdd) {
        // check if the optional AOT dependency is available on the classpath
        try {
            List<?> meanings = (List<?>) RussianUtils.getLookupForMeaningsMethod().invokeExact(word);
            // empty for non-Russian words
            if (meanings.isEmpty()) {
                return;
            }

            for (Object meaning : meanings) {
                Object lemma = RussianUtils.getGetLemmaMethod().invoke(meaning);
                String lemmaString = lemma.toString();

                addRawNgrams(lemmaString, startEachWordOffset, endEachWordOffset,
                        minNgramLength, maxNgramLength, whereToAdd);
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Morphological analysis failed: " + t.getMessage(), t);
        }
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

        // set initial capacity (adjusted by the load factor) to avoid rehashing
        SequencedSet<String> result = LinkedHashSet.newLinkedHashSet(config.getMaxNgramCount());
        int count = 0;

        // for-each is more efficient than iterator
        for (String ngram : ngrams) {
            if (count >= config.getMaxNgramCount()) {
                break;
            }

            result.add(ngram);
            count++;
        }

        return result;
    }

    public enum Mode {

        /**
         * Create both prefix and infix ngrams.
         */
        ALL,

        /**
         * Create prefix (i.e. those starting with the 1st character) ngrams only.
         */
        PREFIX,

        /**
         * Create infix (i.e. those starting with the 2nd character) ngrams only.
         */
        INFIX

    }

}
