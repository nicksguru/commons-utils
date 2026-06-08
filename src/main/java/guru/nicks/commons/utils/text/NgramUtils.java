package guru.nicks.commons.utils.text;

import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import rita.RiTa;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

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
     * Checks if the word is an English stop word: 'the', 'a', 'was', 'it', etc.
     *
     * @param word word to check (will be converted to lowercase with leading and trailing whitespace trimmed)
     * @return {@code true} if the word is a stop word
     */
    public static boolean englishStopWord(String word) {
        if (StringUtils.isBlank(word)) {
            return false;
        }

        return EnglishMorphMethods.stopWord(word.strip().toLowerCase());
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
        if (config.tryEnglishMorphAnalysis() && EnglishMorphMethods.stopWord(word)) {
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
        String lemma = EnglishMorphMethods.lemmatize(word);
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
        RussianMorphMethods morphMethods = RussianMorphMethods.getMethodsOrNull();
        if (morphMethods == null) {
            return;
        }

        try {
            // empty list for unknown words, for example for any non-Russian ones
            List<?> meanings = (List<?>) morphMethods.lookupForMeanings().invokeExact(word);

            if (meanings.isEmpty()) {
                return;
            }

            for (Object meaning : meanings) {
                Object lemma = morphMethods.getLemma().invoke(meaning);
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

    /**
     * Holder for cached method handles used in Russian morphological analysis. {@link MethodHandle} is a faster
     * invocation than reflection.
     *
     * @param lookupForMeanings method handle for {@code WordformMeaning.lookupForMeanings(String)}
     * @param getLemma          method handle for {@code WordformMeaning.getLemma()}
     */
    private record RussianMorphMethods(

            MethodHandle lookupForMeanings,
            MethodHandle getLemma) {

        /**
         * Cached {@code WordformMeaning} class and method handles, or {@code null} if not available. Uses
         * {@link AtomicReference} for thread-safe lazy initialization.
         */
        private static final AtomicReference<RussianMorphMethods> INSTANCE = new AtomicReference<>();

        /**
         * Sentinel value to indicate that initialization failed.
         */
        private static final RussianMorphMethods FAILED = new RussianMorphMethods(null, null);

        /**
         * Checks if {@code com.github.demidko.aot.WordformMeaning} is available on the classpath and caches the result
         * along with method handles for performance. Uses {@link AtomicReference} for thread-safe lazy initialization.
         *
         * @return the handles if available, {@code null} otherwise
         */
        @Nullable
        static RussianMorphMethods getMethodsOrNull() {
            // fast path - already initialized or failed
            RussianMorphMethods morphMethods = INSTANCE.get();
            if ((morphMethods != null) || initializationFailed()) {
                return morphMethods;
            }

            // attempt initialization
            morphMethods = tryInitializeMethods();

            if (morphMethods != null) {
                INSTANCE.compareAndSet(null, morphMethods);
                // if another thread won the race, use its value
                return INSTANCE.get();
            }

            // initialization failed, mark as such
            markInitializationFailed();
            return null;
        }

        /**
         * Attempts to initialize morphology handles by loading the class and creating method handles.
         *
         * @return the handles if successful, {@code null} otherwise
         */
        @Nullable
        private static RussianMorphMethods tryInitializeMethods() {
            try {
                Class<?> wordformMeaningClass = Class.forName("com.github.demidko.aot.WordformMeaning");
                var lookup = MethodHandles.lookup();

                // create method handle for static method: 'List WordformMeaning.lookupForMeanings(String)'
                var lookupForMeanings = lookup.findStatic(wordformMeaningClass, "lookupForMeanings",
                        MethodType.methodType(List.class, String.class));

                // create method handle for virtual method: 'WordformMeaning getLemma()'
                var getLemma = lookup.findVirtual(wordformMeaningClass, "getLemma",
                        MethodType.methodType(Class.forName("com.github.demidko.aot.WordformMeaning")));

                return new RussianMorphMethods(lookupForMeanings, getLemma);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
                return null;
            }
        }

        /**
         * Checks if initialization has previously failed.
         *
         * @return {@code true} if initialization failed, {@code false} otherwise
         */
        private static boolean initializationFailed() {
            return INSTANCE.get() == FAILED;
        }

        /**
         * Marks initialization as failed to avoid repeated attempts.
         */
        private static void markInitializationFailed() {
            INSTANCE.compareAndSet(null, RussianMorphMethods.FAILED);
        }

    }

    /**
     * @see #lemmatize(String)
     */
    private static class EnglishMorphMethods {

        /**
         * Common English stop words (NOT filtered out during lemmatization).
         */
        private static final Set<String> STOP_WORDS = Set.of(
                "a", "an", "the",
                "and", "or", "but", "if", "while",
                "in", "on", "at", "to", "for", "of", "with", "by", "from", "as", "into",
                "is", "are", "was", "were", "be", "been", "being",
                "have", "has", "had", "do", "does", "did",
                "this", "that", "these", "those",
                "i", "you", "he", "she", "it", "we", "they",
                "me", "him", "her", "us", "them",
                "my", "your", "his", "its", "our", "their");

        /**
         * Maps many (but not all) irregular English forms to their base lemmas, such as 'be' for 'was'. This covers the
         * gaps that standard algorithmic stemmers (like Postgres Snowball) miss.
         */
        private static final Map<String, String> IRREGULARS = HashMap.newHashMap(350);

        static {
            // ==========================================
            // IRREGULAR VERBS (Past / Participle / 3rd Person -> Infinitive)
            // ==========================================
            // Be
            IRREGULARS.put("was", "be");
            IRREGULARS.put("were", "be");
            IRREGULARS.put("am", "be");
            IRREGULARS.put("is", "be");
            IRREGULARS.put("are", "be");
            IRREGULARS.put("been", "be");
            // Have
            IRREGULARS.put("had", "have");
            IRREGULARS.put("has", "have");
            // Do
            IRREGULARS.put("did", "do");
            IRREGULARS.put("does", "do");
            IRREGULARS.put("done", "do");
            // Go
            IRREGULARS.put("went", "go");
            IRREGULARS.put("gone", "go");
            IRREGULARS.put("goes", "go");
            // Common irregulars
            IRREGULARS.put("ran", "run");
            IRREGULARS.put("runs", "run");
            IRREGULARS.put("ate", "eat");
            IRREGULARS.put("eaten", "eat");
            IRREGULARS.put("eats", "eat");
            IRREGULARS.put("saw", "see");
            IRREGULARS.put("seen", "see");
            IRREGULARS.put("sees", "see");
            IRREGULARS.put("came", "come");
            IRREGULARS.put("comes", "come");
            IRREGULARS.put("took", "take");
            IRREGULARS.put("taken", "take");
            IRREGULARS.put("takes", "take");
            IRREGULARS.put("made", "make");
            IRREGULARS.put("makes", "make");
            IRREGULARS.put("gave", "give");
            IRREGULARS.put("given", "give");
            IRREGULARS.put("gives", "give");
            IRREGULARS.put("knew", "know");
            IRREGULARS.put("known", "know");
            IRREGULARS.put("knows", "know");
            IRREGULARS.put("got", "get");
            IRREGULARS.put("gotten", "get");
            IRREGULARS.put("gets", "get");
            IRREGULARS.put("found", "find");
            IRREGULARS.put("finds", "find");
            IRREGULARS.put("thought", "think");
            IRREGULARS.put("thinks", "think");
            IRREGULARS.put("told", "tell");
            IRREGULARS.put("tells", "tell");
            IRREGULARS.put("became", "become");
            IRREGULARS.put("becomes", "become");
            IRREGULARS.put("left", "leave");
            IRREGULARS.put("leaves", "leave"); // "leaves" as noun handled below
            IRREGULARS.put("felt", "feel");
            IRREGULARS.put("feels", "feel");
            IRREGULARS.put("brought", "bring");
            IRREGULARS.put("brings", "bring");
            IRREGULARS.put("began", "begin");
            IRREGULARS.put("begun", "begin");
            IRREGULARS.put("begins", "begin");
            IRREGULARS.put("kept", "keep");
            IRREGULARS.put("keeps", "keep");
            IRREGULARS.put("held", "hold");
            IRREGULARS.put("holds", "hold");
            IRREGULARS.put("wrote", "write");
            IRREGULARS.put("written", "write");
            IRREGULARS.put("writes", "write");
            IRREGULARS.put("stood", "stand");
            IRREGULARS.put("stands", "stand");
            IRREGULARS.put("heard", "hear");
            IRREGULARS.put("hears", "hear");
            IRREGULARS.put("meant", "mean");
            IRREGULARS.put("means", "mean");
            IRREGULARS.put("set", "set");
            IRREGULARS.put("sets", "set");
            IRREGULARS.put("met", "meet");
            IRREGULARS.put("meets", "meet");
            IRREGULARS.put("paid", "pay");
            IRREGULARS.put("pays", "pay");
            IRREGULARS.put("sat", "sit");
            IRREGULARS.put("sits", "sit");
            IRREGULARS.put("spoke", "speak");
            IRREGULARS.put("spoken", "speak");
            IRREGULARS.put("speaks", "speak");
            IRREGULARS.put("led", "lead");
            IRREGULARS.put("leads", "lead"); // "lead" as noun handled below
            IRREGULARS.put("read", "read");
            IRREGULARS.put("reads", "read");
            IRREGULARS.put("grew", "grow");
            IRREGULARS.put("grown", "grow");
            IRREGULARS.put("grows", "grow");
            IRREGULARS.put("lost", "lose");
            IRREGULARS.put("loses", "lose");
            IRREGULARS.put("fell", "fall");
            IRREGULARS.put("fallen", "fall");
            IRREGULARS.put("falls", "fall");
            IRREGULARS.put("sent", "send");
            IRREGULARS.put("sends", "send");
            IRREGULARS.put("built", "build");
            IRREGULARS.put("builds", "build");
            IRREGULARS.put("understood", "understand");
            IRREGULARS.put("understands", "understand");
            IRREGULARS.put("cut", "cut");
            IRREGULARS.put("cuts", "cut");
            IRREGULARS.put("put", "put");
            IRREGULARS.put("puts", "put");
            IRREGULARS.put("hit", "hit");
            IRREGULARS.put("hits", "hit");
            IRREGULARS.put("bought", "buy");
            IRREGULARS.put("buys", "buy");
            IRREGULARS.put("caught", "catch");
            IRREGULARS.put("catches", "catch");
            IRREGULARS.put("drew", "draw");
            IRREGULARS.put("drawn", "draw");
            IRREGULARS.put("draws", "draw");
            IRREGULARS.put("drove", "drive");
            IRREGULARS.put("driven", "drive");
            IRREGULARS.put("drives", "drive");
            IRREGULARS.put("broke", "break");
            IRREGULARS.put("broken", "break");
            IRREGULARS.put("breaks", "break");
            IRREGULARS.put("chose", "choose");
            IRREGULARS.put("chosen", "choose");
            IRREGULARS.put("chooses", "choose");
            IRREGULARS.put("drank", "drink");
            IRREGULARS.put("drunk", "drink");
            IRREGULARS.put("drinks", "drink");
            IRREGULARS.put("flew", "fly");
            IRREGULARS.put("flown", "fly");
            IRREGULARS.put("flies", "fly");
            IRREGULARS.put("swam", "swim");
            IRREGULARS.put("swum", "swim");
            IRREGULARS.put("swims", "swim");
            IRREGULARS.put("rang", "ring");
            IRREGULARS.put("rung", "ring");
            IRREGULARS.put("rings", "ring");
            IRREGULARS.put("sang", "sing");
            IRREGULARS.put("sung", "sing");
            IRREGULARS.put("sings", "sing");
            IRREGULARS.put("sank", "sink");
            IRREGULARS.put("sunk", "sink");
            IRREGULARS.put("sinks", "sink");
            IRREGULARS.put("shook", "shake");
            IRREGULARS.put("shaken", "shake");
            IRREGULARS.put("shakes", "shake");
            IRREGULARS.put("stole", "steal");
            IRREGULARS.put("stolen", "steal");
            IRREGULARS.put("steals", "steal");
            IRREGULARS.put("swore", "swear");
            IRREGULARS.put("sworn", "swear");
            IRREGULARS.put("swears", "swear");
            IRREGULARS.put("threw", "throw");
            IRREGULARS.put("thrown", "throw");
            IRREGULARS.put("throws", "throw");
            IRREGULARS.put("wore", "wear");
            IRREGULARS.put("worn", "wear");
            IRREGULARS.put("wears", "wear");
            IRREGULARS.put("bit", "bite");
            IRREGULARS.put("bitten", "bite");
            IRREGULARS.put("bites", "bite");
            IRREGULARS.put("hid", "hide");
            IRREGULARS.put("hidden", "hide");
            IRREGULARS.put("hides", "hide");
            IRREGULARS.put("froze", "freeze");
            IRREGULARS.put("frozen", "freeze");
            IRREGULARS.put("freezes", "freeze");
            IRREGULARS.put("rose", "rise");
            IRREGULARS.put("risen", "rise");
            IRREGULARS.put("rises", "rise");
            IRREGULARS.put("woke", "wake");
            IRREGULARS.put("woken", "wake");
            IRREGULARS.put("wakes", "wake");
            IRREGULARS.put("wove", "weave");
            IRREGULARS.put("woven", "weave");
            IRREGULARS.put("weaves", "weave");
            IRREGULARS.put("tore", "tear");
            IRREGULARS.put("torn", "tear");
            IRREGULARS.put("tears", "tear");
            IRREGULARS.put("shrank", "shrink");
            IRREGULARS.put("shrunk", "shrink");
            IRREGULARS.put("shrinks", "shrink");
            IRREGULARS.put("struck", "strike");
            IRREGULARS.put("strikes", "strike");
            IRREGULARS.put("sought", "seek");
            IRREGULARS.put("seeks", "seek");
            IRREGULARS.put("fought", "fight");
            IRREGULARS.put("fights", "fight");
            IRREGULARS.put("bound", "bind");
            IRREGULARS.put("binds", "bind");
            IRREGULARS.put("ground", "grind");
            IRREGULARS.put("grinds", "grind");
            IRREGULARS.put("wound", "wind");
            IRREGULARS.put("winds", "wind");
            IRREGULARS.put("spun", "spin");
            IRREGULARS.put("spins", "spin");
            IRREGULARS.put("clung", "cling");
            IRREGULARS.put("clings", "cling");
            IRREGULARS.put("stung", "sting");
            IRREGULARS.put("stings", "sting");

            IRREGULARS.put("swung", "swing");
            IRREGULARS.put("swings", "swing");
            IRREGULARS.put("wrung", "wring");
            IRREGULARS.put("wrings", "wring");
            IRREGULARS.put("slung", "sling");
            IRREGULARS.put("slings", "sling");
            IRREGULARS.put("stuck", "stick");
            IRREGULARS.put("sticks", "stick");
            IRREGULARS.put("dealt", "deal");
            IRREGULARS.put("deals", "deal");
            IRREGULARS.put("knelt", "kneel");
            IRREGULARS.put("kneels", "kneel");
            IRREGULARS.put("leant", "lean");
            IRREGULARS.put("leans", "lean");
            IRREGULARS.put("leapt", "leap");
            IRREGULARS.put("leaps", "leap");
            IRREGULARS.put("crept", "creep");
            IRREGULARS.put("creeps", "creep");
            IRREGULARS.put("wept", "weep");
            IRREGULARS.put("weeps", "weep");
            IRREGULARS.put("slept", "sleep");
            IRREGULARS.put("sleeps", "sleep");
            IRREGULARS.put("swept", "sweep");
            IRREGULARS.put("sweeps", "sweep");
            IRREGULARS.put("fed", "feed");
            IRREGULARS.put("feeds", "feed");
            IRREGULARS.put("bred", "breed");
            IRREGULARS.put("breeds", "breed");
            IRREGULARS.put("bled", "bleed");
            IRREGULARS.put("bleeds", "bleed");
            IRREGULARS.put("fled", "flee");
            IRREGULARS.put("flees", "flee");
            IRREGULARS.put("sped", "speed");
            IRREGULARS.put("speeds", "speed");
            IRREGULARS.put("shed", "shed");
            IRREGULARS.put("sheds", "shed");
            IRREGULARS.put("spread", "spread");
            IRREGULARS.put("spreads", "spread");
            IRREGULARS.put("bet", "bet");
            IRREGULARS.put("bets", "bet");
            IRREGULARS.put("cast", "cast");
            IRREGULARS.put("casts", "cast");
            IRREGULARS.put("cost", "cost");
            IRREGULARS.put("costs", "cost");
            IRREGULARS.put("shut", "shut");
            IRREGULARS.put("shuts", "shut");
            IRREGULARS.put("split", "split");
            IRREGULARS.put("splits", "split");
            IRREGULARS.put("let", "let");
            IRREGULARS.put("lets", "let");
            IRREGULARS.put("burst", "burst");
            IRREGULARS.put("bursts", "burst");
            IRREGULARS.put("hung", "hang");
            IRREGULARS.put("hangs", "hang");
            IRREGULARS.put("spat", "spit");
            IRREGULARS.put("spits", "spit");
            IRREGULARS.put("lit", "light");
            IRREGULARS.put("lights", "light");
            IRREGULARS.put("bid", "bid");
            IRREGULARS.put("bids", "bid");

            // ==========================================
            // IRREGULAR NOUNS (Plural -> Singular)
            // ==========================================
            // Vowel changes / Old English
            IRREGULARS.put("men", "man");
            IRREGULARS.put("women", "woman");
            IRREGULARS.put("children", "child");
            IRREGULARS.put("oxen", "ox");
            IRREGULARS.put("feet", "foot");
            IRREGULARS.put("geese", "goose");
            IRREGULARS.put("teeth", "tooth");
            IRREGULARS.put("mice", "mouse");
            IRREGULARS.put("lice", "louse");
            IRREGULARS.put("brethren", "brother");

            // Latin/Greek origin plurals (Common in technical/academic search)
            IRREGULARS.put("analyses", "analysis");
            IRREGULARS.put("bases", "base"); // Also plural of basis, but base is a safer lemma for search
            IRREGULARS.put("crises", "crisis");
            IRREGULARS.put("diagnoses", "diagnosis");
            IRREGULARS.put("hypotheses", "hypothesis");
            IRREGULARS.put("oases", "oasis");
            IRREGULARS.put("parentheses", "parenthesis");
            IRREGULARS.put("theses", "thesis");
            IRREGULARS.put("axes", "axis");
            IRREGULARS.put("phenomena", "phenomenon");
            IRREGULARS.put("criteria", "criterion");
            IRREGULARS.put("data", "datum"); // Often treated as mass noun, but mathematically correct
            IRREGULARS.put("media", "medium");
            IRREGULARS.put("bacteria", "bacterium");
            IRREGULARS.put("curricula", "curriculum");
            IRREGULARS.put("memoranda", "memorandum");
            IRREGULARS.put("strata", "stratum");
            IRREGULARS.put("alumni", "alumnus");
            IRREGULARS.put("cacti", "cactus"); // cactuses also valid, but cacti common
            IRREGULARS.put("foci", "focus");
            IRREGULARS.put("fungi", "fungus");
            IRREGULARS.put("nuclei", "nucleus");
            IRREGULARS.put("radii", "radius");
            IRREGULARS.put("stimuli", "stimulus");
            IRREGULARS.put("syllabi", "syllabus");
            IRREGULARS.put("appendices", "appendix");
            IRREGULARS.put("indices", "index"); // indexes also valid
            IRREGULARS.put("matrices", "matrix");
            IRREGULARS.put("vertices", "vertex");
            IRREGULARS.put("bureaux", "bureau"); // bureaus also valid
            IRREGULARS.put("plateaux", "plateau");
            IRREGULARS.put("tableaux", "tableau");

            // Unchanging / Zero Plurals (Search indexes benefit from explicit mapping here)
            IRREGULARS.put("sheep", "sheep");
            IRREGULARS.put("deer", "deer");
            IRREGULARS.put("fish", "fish"); // fishes exists for species, but fish is primary
            IRREGULARS.put("species", "species");
            IRREGULARS.put("series", "series");
            IRREGULARS.put("aircraft", "aircraft");
            IRREGULARS.put("moose", "moose");
            IRREGULARS.put("swine", "swine");

            // ==========================================
            // IRREGULAR ADJECTIVES (Comparative/Superlative -> Base)
            // ==========================================
            IRREGULARS.put("better", "good");
            IRREGULARS.put("best", "good");
            IRREGULARS.put("worse", "bad");
            IRREGULARS.put("worst", "bad");
            IRREGULARS.put("less", "little");
            IRREGULARS.put("least", "little");
            IRREGULARS.put("further", "far");
            IRREGULARS.put("farthest", "far");
            IRREGULARS.put("furthest", "far");
            IRREGULARS.put("elder", "old");
            IRREGULARS.put("eldest", "old");
            IRREGULARS.put("more", "much");
            IRREGULARS.put("most", "much");
        }

        /**
         * Checks for common stop words, such as 'the', 'a'.
         *
         * @param word must be non-blank and in lowercase already, for speed reasons
         * @return {@code true} if the word is a stop word
         */
        static boolean stopWord(String word) {
            return EnglishMorphMethods.STOP_WORDS.contains(word);
        }

        /**
         * Converts English words to their base forms, taking into account some (but not all) irregular words. The
         * common stop words are not filtered out, rather processed, such as 'was' becomes 'be'.
         *
         * @param word must be non-blank in lowercase already, for speed reasons
         * @return base word form (lemma)
         */
        static String lemmatize(String word) {
            String stem = RiTa.stem(word);
            String regular = IRREGULARS.get(stem);

            return StringUtils.isNotBlank(regular)
                    ? regular
                    : stem;
        }

    }

}
