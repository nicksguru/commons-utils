package guru.nicks.utils;

/**
 * {@link NgramUtils} settings.
 */
public interface NgramUtilsConfig {

    NgramUtilsConfig DEFAULT = new NgramUtilsConfig() {
    };

    /**
     * Full text search DB indexes may or may not support collation (Mongo does not). To treat, during search, accents
     * as non-accents, they can be reduced to their base characters: {@code ä -> a}.
     */
    default boolean isReduceAccents() {
        return true;
    }

    /**
     * Try morphological analysis to add stem ngrams to plain ones. To be precise, those are not just ordinary stems,
     * but 'smart stems' - in some languages, singular and plural forms of the same word are totally different words.
     * <p>
     * The only language supported is Russian, and the size of the dictionary read to RAM is 110Mb. For English, there's
     * no such need - infix ngrams are sufficient.
     *
     * @return {@code false} by default
     */
    default boolean tryMorphAnalysis() {
        return false;
    }

    /**
     * Limits memory consumption when parsing long texts.
     *
     * @return {@link Short#MAX_VALUE} by default
     */
    default int getMaxNgramCount() {
        return Short.MAX_VALUE;
    }

    /**
     * @return minimum ngram length, 3 by default (1 or 2 yield too many irrelevant search hits and increase DB size)
     */
    default int getMinNgramLength() {
        return 3;
    }

    /**
     * @return 12 by default
     */
    default int getMaxPrefixNgramLength() {
        return 12;
    }

    /**
     * For example, a 19-letter word 'incomprehensibility', if min. ngram length is 1, yields 1+2+3+...+19 = 190 prefix
     * ngrams; then, starting with the second letter, 189 infix ngrams and so on, making a total of 3439 ngrams - and
     * all this for a single word.
     * <p>
     * To alleviate that, infix ngrams are shorter than prefix ones - because their number is greater and priority is
     * lower.
     *
     * @return 3 by default
     */
    default int getMaxInfixNgramLength() {
        return 3;
    }

    /**
     * Returns the maximum number of threads to use for word processing.
     *
     * @return 10 by default
     *
     */
    default int getMaxThreads() {
        return 10;
    }

}
