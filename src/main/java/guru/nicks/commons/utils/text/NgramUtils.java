package guru.nicks.commons.utils.text;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.LinkedHashSet;
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
     * Creates ngrams for unique words extracted from the input string. The string is tokenized exactly once, see
     * {@link #createNgrams(Set, Mode, NgramUtilsConfig)} for the ngram generation itself.
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
        // tokenize once - both ngram phases below reuse the same word set
        Set<String> uniqueWords = TextUtils.collectUniqueWords(str, config.isReduceAccents());
        return createNgrams(uniqueWords, mode, config);
    }

    /**
     * Creates ngrams for pre-tokenized unique words, sparing the caller the cost of re-tokenizing the same text. Words
     * are processed in the iteration order of the given collection, so passing the sorted set from
     * {@link TextUtils#collectUniqueWords(String, boolean)} yields output identical to
     * {@link #createNgrams(String, Mode, NgramUtilsConfig)}.
     * <p>
     * WARNING: original words are part of their ngrams only if their length is within the ngram length limits. As such,
     * the word 'ox' is too short for trigrams.
     *
     * @param uniqueWords unique words to create ngrams for, <b>already lowercased (and with accents reduced, if</b>
     *                    {@link NgramUtilsConfig#isReduceAccents()} is on)
     * @param mode        mode of ngrams creation
     * @param config      configuration
     * @return ngrams (modifiable collection, max. {@link NgramUtilsConfig#getMaxNgramCount()} elements to avoid memory
     *         overflow, prefix ngrams go first - they have precedence before collection truncation)
     */
    public static SequencedSet<String> createNgrams(Set<String> uniqueWords, Mode mode, NgramUtilsConfig config) {
        // hoisted once per call - the early-exit checks inside generation compare the set size against it
        int maxNgramCount = config.getMaxNgramCount();

        SequencedSet<String> ngrams = switch (mode) {
            case ALL -> {
                SequencedSet<String> prefixNgrams = createPrefixNgrams(uniqueWords, config);

                // prefix phase saturated the cap - infix ngrams would be discarded entirely, so skip generating
                // them; otherwise infix ngrams fill the remaining slots, stopping early once the cap is reached
                if (prefixNgrams.size() < maxNgramCount) {
                    addNgrams(prefixNgrams, uniqueWords, config, 1, Integer.MAX_VALUE);
                }

                yield prefixNgrams;
            }

            case PREFIX -> createPrefixNgrams(uniqueWords, config);
            case INFIX -> createInfixNgrams(uniqueWords, config);
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
     * @param words  unique words to process
     * @param config configuration
     * @return ngrams (modifiable collection, max. {@link NgramUtilsConfig#getMaxNgramCount()} items)
     */
    private static SequencedSet<String> createPrefixNgrams(Collection<String> words, NgramUtilsConfig config) {
        return generateNgrams(words, config, 0, 0);
    }

    /**
     * Creates prefix ngrams - processes each word starting with its 2nd letter, for example trigrams: 'strings' ->
     * 'tri', 'rin', 'ing', 'ngs'.
     * <p>
     * WARNING: original words are part of the result only if the word length is within the ngram length limits.
     *
     * @param words  unique words to process
     * @param config configuration
     * @return ngrams (modifiable collection, max. {@link NgramUtilsConfig#getMaxNgramCount()} items)
     */
    private static SequencedSet<String> createInfixNgrams(Collection<String> words, NgramUtilsConfig config) {
        return generateNgrams(words, config, 1, Integer.MAX_VALUE);
    }

    /**
     * Creates ngrams for each unique word.
     * <p>
     * WARNING: original words are part of the result only if the word length is within the ngram length limits and
     * {@code startEachWordOffset} is 0.
     *
     * @param words               unique words to process
     * @param config              configuration
     * @param startEachWordOffset offset in each word to start at
     * @param endEachWordOffset   offset in each word to finish at (word lengths differ, so pass
     *                            {@link Integer#MAX_VALUE} to process each word fully)
     * @return ngrams (modifiable collection, max. {@link NgramUtilsConfig#getMaxNgramCount()} items)
     */
    private static SequencedSet<String> generateNgrams(Collection<String> words, NgramUtilsConfig config,
            int startEachWordOffset, int endEachWordOffset) {
        SequencedSet<String> ngrams = LinkedHashSet.newLinkedHashSet(words.size() * ASSUMED_NGRAMS_PER_WORD);
        addNgrams(ngrams, words, config, startEachWordOffset, endEachWordOffset);
        // safety net - with the early exit inside addNgrams the set never exceeds the cap, so no copy happens
        return limitNgramCount(ngrams, config);
    }

    /**
     * Called from {@link #generateNgrams(Collection, NgramUtilsConfig, int, int)} and from
     * {@link #createNgrams(Set, Mode, NgramUtilsConfig)} (to fill the remaining slots of an already populated set). See
     * description of arguments there.
     * <p>
     * Generation stops early once the set holds {@link NgramUtilsConfig#getMaxNgramCount()} distinct ngrams -
     * everything generated beyond the cap would be truncated afterward anyway, so producing it is pure waste.
     *
     * @param ngrams set to add the ngrams to, its current content counts towards the cap
     */
    private static void addNgrams(Set<String> ngrams, Collection<String> words, NgramUtilsConfig config,
            int startEachWordOffset, int endEachWordOffset) {
        // hoisted once per phase - the early-exit checks below compare the set size against it
        int maxNgramCount = config.getMaxNgramCount();

        // 0 means prefix ngrams are to be generated
        int maxNgramLength = (startEachWordOffset == 0)
                ? config.getMaxPrefixNgramLength()
                : config.getMaxInfixNgramLength();

        for (String word : words) {
            // early exit - the cap of distinct ngrams is reached, all further ngrams would be truncated anyway
            if (ngrams.size() >= maxNgramCount) {
                break;
            }

            addWordNgrams(config, startEachWordOffset, endEachWordOffset, word, maxNgramLength, maxNgramCount, ngrams);
        }
    }

    /**
     * Called from {@link #addNgrams(Set, Collection, NgramUtilsConfig, int, int)}. See description of arguments there.
     */
    private static void addWordNgrams(NgramUtilsConfig config, int startEachWordOffset, int endEachWordOffset,
            String word, int maxNgramLength, int maxNgramCount, Set<String> whereToAdd) {
        // special case: English stop words don't make their way into ANY ngrams; fast path - words are already
        // lowercase and trimmed per the createNgrams(Collection, ...) contract
        if (config.tryEnglishMorphAnalysis() && EnglishUtils.stopWord(word, true)) {
            return;
        }

        addRawNgrams(word, startEachWordOffset, endEachWordOffset,
                config.getMinNgramLength(), maxNgramLength, maxNgramCount, whereToAdd);

        if (config.tryEnglishMorphAnalysis()) {
            addEnglishMorphNgrams(word, startEachWordOffset, endEachWordOffset,
                    config.getMinNgramLength(), maxNgramLength, maxNgramCount, whereToAdd);
        }

        if (config.tryRussianMorphAnalysis()) {
            addRussianMorphNgrams(word, startEachWordOffset, endEachWordOffset,
                    config.getMinNgramLength(), maxNgramLength, maxNgramCount, whereToAdd);
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
     * @param maxNgramCount  maximum number of distinct ngrams to hold - generation stops early once it is reached
     * @param whereToAdd     where to add the ngrams
     */
    private static void addRawNgrams(String word, int startOffset, int endOffset,
            int minNgramLength, int maxNgramLength, int maxNgramCount, Set<String> whereToAdd) {
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

                // early exit - the cap of distinct ngrams is reached, everything further would be truncated
                // anyway, so even the substring is not worth allocating
                if (whereToAdd.size() >= maxNgramCount) {
                    return;
                }

                whereToAdd.add(word.substring(i, i + ngramLength));
            }
        }
    }

    /**
     * Performs morphology analysis for English and creates ngrams for the 'lemma' (kind of word stem, but smarter). For
     * arguments and return value, see {@link #addRawNgrams(String, int, int, int, int, int, Set)}. The common stop
     * words (such as 'the', be') are NOT filtered out, rather processed ('was' becomes 'be' etc.).
     * <p>
     * This method is light-weight, requires no dictionary, converts 'ran' to 'run', 'geese' to 'goose' and so on.
     * <p>
     * WARNING: the lemma is only processed if its length is within the ngram length and word offset limits. For
     * example, 'was' becomes 'be' whose length (2) is smaller than the common minimum ngram length (3). Also, the lemma
     * is skipped if it is the same as the original word - because 'raw ngrams' do the same.
     *
     * @param word must be non-blank and in lowercase already, for speed reasons
     */
    private static void addEnglishMorphNgrams(String word, int startEachWordOffset, int endEachWordOffset,
            int minNgramLength, int maxNgramLength, int maxNgramCount, Set<String> whereToAdd) {
        String lemma = EnglishUtils.getWordLemma(word);

        if (!lemma.equals(word)) {
            addRawNgrams(lemma, startEachWordOffset, endEachWordOffset,
                    minNgramLength, maxNgramLength, maxNgramCount, whereToAdd);
        }
    }

    /**
     * Performs morphology analysis for Russian and creates ngrams for the 'lemma' (kind of word stem, but smarter). For
     * arguments and return value, see {@link #addRawNgrams(String, int, int, int, int, int, Set)}.
     * <p>
     * WARNING: the lemma is only processed (split into ngrams) if its length is within the ngram length and word offset
     * limits. Also, if the lemma is the same as the original word, it is skipped - because 'raw ngrams' do the same.
     */
    private static void addRussianMorphNgrams(String word, int startEachWordOffset, int endEachWordOffset,
            int minNgramLength, int maxNgramLength, int maxNgramCount, Set<String> whereToAdd) {
        String lemma = RussianUtils.getWordLemma(word);

        if (!lemma.equals(word)) {
            addRawNgrams(lemma, startEachWordOffset, endEachWordOffset,
                    minNgramLength, maxNgramLength, maxNgramCount, whereToAdd);
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
