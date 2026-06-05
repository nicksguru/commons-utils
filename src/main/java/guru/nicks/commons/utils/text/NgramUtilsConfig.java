package guru.nicks.commons.utils.text;

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
     * Try morphological analysis to add (not replace!) English stem ngrams to plain ones. To be precise, those are not
     * just ordinary stems, but lemmas: 'ran' is converted to 'run', 'geese' to 'goose' and so on.
     * <p>
     * There's no dictionary read to RAM, the procedure is fast and lightweight and therefore is on by default.
     *
     * @return {@code false} by default
     */
    default boolean tryEnglishMorphAnalysis() {
        return true;
    }

    /**
     * Try morphological analysis to add (not replace!) Russian stem ngrams to plain ones. To be precise, those are not
     * just ordinary stems, but lemmas - in some languages, singular and plural forms of the same word are totally
     * different words.
     * <p>
     * The dictionary size read to RAM is 110Mb.
     *
     * @return {@code false} by default
     */
    default boolean tryRussianMorphAnalysis() {
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
