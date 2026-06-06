package guru.nicks.commons.utils.text;

import guru.nicks.commons.utils.FutureUtils;

import am.ik.yavi.meta.ConstraintArguments;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import rita.RiTa;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;

/**
 * Ngram-related utility methods. To search against ngrams, the search text should be split into n-grams too (usually
 * with {@link #createNgrams(String, Mode, NgramUtilsConfig)}). The more ngrams match against each other, the higher is
 * the search score.
 * <p>
 * English words are, if {@link NgramUtilsConfig#tryEnglishMorphAnalysis()} is on, augmented with their singular
 * 'lemmas' (ran → run, geese → goose, etc.) with stop words ('the', 'a', 'be', etc.) filtered out.
 * <p>
 * Russian words are, if {@link NgramUtilsConfig#tryRussianMorphAnalysis()} is on, augmented with their singular
 * 'lemmas' (morphologically analyzed forms, not just stems), for example: 'люди' ('humans') →- 'человек' ('a human').
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
                // downgrade sorted set to linked one, so infix ngrams will be added to its tail
                SequencedSet<String> prefixNgrams = new LinkedHashSet<>(createPrefixNgrams(str, config));
                SequencedSet<String> infixNgrams = createInfixNgrams(str, config);

                // temporarily, this set may hold two times the max. ngram count
                if (prefixNgrams.size() < config.getMaxNgramCount()) {
                    prefixNgrams.addAll(infixNgrams);
                }

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
     *         overflow, prefix ngrams go first - they have precedence before collection truncation)
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
     *         overflow, prefix ngrams go first - they have precedence before collection truncation)
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
     * @return ngrams (modifiable collection, max. {@link NgramUtilsConfig#getMaxNgramCount()} elements to avoid memory
     *         overflow, prefix ngrams go first - they have precedence before collection truncation)
     */
    private static SequencedSet<String> generateNgrams(String str, NgramUtilsConfig config,
            int startEachWordOffset, int endEachWordOffset) {
        // avoid processing the same word twice
        Set<String> words = TextUtils.collectUniqueWords(str, config.isReduceAccents());

        int maxNgramLength = (startEachWordOffset == 0)
                ? config.getMaxPrefixNgramLength()
                : config.getMaxInfixNgramLength();
        var ngrams = new TreeSet<String>();

        // Process words in parallel according to settings. Don't pre-create a supplier for each word - the number
        // of words may be large. Instead, create suppliers in chunks.
        for (var iterator = words.iterator(); iterator.hasNext(); ) {
            var wordProcessors = new ArrayList<Supplier<Set<String>>>(config.getMaxThreads());

            for (int i = 0; (i < config.getMaxThreads()) && iterator.hasNext(); i++) {
                String word = iterator.next();

                wordProcessors.add(() -> {
                    // special case: English stop words don't make their way into plain ngrams
                    if (config.tryEnglishMorphAnalysis() && EnglishMorphMethods.isStopWord(word)) {
                        return Collections.emptySortedSet();
                    }

                    SortedSet<String> plainNgrams = generatePlainNgrams(word,
                            startEachWordOffset, endEachWordOffset,
                            config.getMinNgramLength(), maxNgramLength);

                    if (config.tryEnglishMorphAnalysis()) {
                        SortedSet<String> morphNgrams = generateEnglishMorphNgrams(word,
                                startEachWordOffset, endEachWordOffset,
                                config.getMinNgramLength(), maxNgramLength);

                        plainNgrams.addAll(morphNgrams);
                    }

                    if (config.tryRussianMorphAnalysis()) {
                        SortedSet<String> morphNgrams = generateRussianMorphNgrams(word,
                                startEachWordOffset, endEachWordOffset,
                                config.getMinNgramLength(), maxNgramLength);

                        plainNgrams.addAll(morphNgrams);
                    }

                    return plainNgrams;
                });
            }

            FutureUtils.getInParallel(wordProcessors)
                    .forEach(ngrams::addAll);
        }

        return limitNgramCount(ngrams, config);
    }

    /**
     * Given a string, generates ngrams for it.
     * <p>
     * WARNING: the original word are part of the result only if its length is within the ngram length limits.
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
    private static SortedSet<String> generatePlainNgrams(String word,
            int startOffset, int endOffset,
            int minNgramLength, int maxNgramLength) {
        check(startOffset, _NgramUtilsGeneratePlainNgramsArgumentsMeta.STARTOFFSET.name()).positiveOrZero();
        check(endOffset, _NgramUtilsGeneratePlainNgramsArgumentsMeta.ENDOFFSET.name()).constraint(it ->
                it >= startOffset, "must be >= " + _NgramUtilsGeneratePlainNgramsArgumentsMeta.STARTOFFSET.name());

        check(minNgramLength, _NgramUtilsGeneratePlainNgramsArgumentsMeta.MINNGRAMLENGTH.name()).positive();
        check(maxNgramLength, _NgramUtilsGeneratePlainNgramsArgumentsMeta.MAXNGRAMLENGTH.name()).constraint(it ->
                        it >= minNgramLength,
                "must be >= " + _NgramUtilsGeneratePlainNgramsArgumentsMeta.MINNGRAMLENGTH.name());

        var ngrams = new TreeSet<String>();

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
     * Performs morphology analysis for English and creates ngrams for the 'lemma' (kind of word stem, but smarter). For
     * arguments and return value, see {@link #generatePlainNgrams(String, int, int, int, int)}. The common stop words
     * (such as 'the', be') are NOT filtered out, rather processed ('was' becomes 'be' etc.).
     * <p>
     * This method is light-weight, requires no dictionary, converts 'ran' to 'run', 'geese' to 'goose' and so on.
     * <p>
     * WARNING: the lemma is only processed if its length is within the ngram length and word offset limits. For
     * example, 'was' becomes 'be' whose length (2) is smaller than the common minimum ngram length (3). In this case,
     * this method returns an empty collection.
     *
     * @param word must be non-blank in lowercase already, for speed reasons
     * @return ngrams (empty unmodifiable collection or non-empty modifiable one)
     */
    private static SortedSet<String> generateEnglishMorphNgrams(String word,
            int startEachWordOffset, int endEachWordOffset,
            int minNgramLength, int maxNgramLength) {
        String lemma = EnglishMorphMethods.lemmatize(word);

        // lemma might be the word itself or a prefix of the original word (i.e. part of plain prefix ngrams already)
        return !Strings.CS.startsWith(word, lemma)
                ? generatePlainNgrams(lemma,
                startEachWordOffset, endEachWordOffset, minNgramLength, maxNgramLength)
                : Collections.emptySortedSet();
    }

    /**
     * Performs morphology analysis for Russian and creates ngrams for the 'lemma' (kind of word stem, but smarter). For
     * arguments and return value, see {@link #generatePlainNgrams(String, int, int, int, int)}.
     * <p>
     * This method only works if {@code com.github.demidko.aot.WordformMeaning} is available on the classpath. If the
     * class is not available, returns an empty collection.
     * <p>
     * WARNING: the lemma is only processed if its length is within the ngram length and word offset limits.
     *
     * @return ngrams (empty unmodifiable collection if morphological analysis is not available - no class on the
     *         classpath - or the word is not a Russian one; modifiable collection otherwise)
     */
    private static SortedSet<String> generateRussianMorphNgrams(String word,
            int startEachWordOffset, int endEachWordOffset,
            int minNgramLength, int maxNgramLength) {
        // check if the optional AOT dependency is available on the classpath
        RussianMorphMethods morphMethods = RussianMorphMethods.getMethodsOrNull();
        if (morphMethods == null) {
            return Collections.emptySortedSet();
        }

        SortedSet<String> ngrams = null;

        try {
            // empty list for unknown words, for example for all non-Russian ones
            List<?> meanings = (List<?>) morphMethods.lookupForMeanings().invokeExact(word);

            for (Object meaning : meanings) {
                Object lemma = morphMethods.getLemma().invoke(meaning);
                String lemmaString = lemma.toString();

                if (ngrams == null) {
                    ngrams = new TreeSet<>();
                }

                // lemma might be the word itself or a prefix of the original word (i.e. part of plain prefix ngrams)
                if (!Strings.CS.startsWith(word, lemmaString)) {
                    Set<String> plainNgrams = generatePlainNgrams(lemmaString,
                            startEachWordOffset, endEachWordOffset,
                            minNgramLength, maxNgramLength);
                    ngrams.addAll(plainNgrams);
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("Morphological analysis failed: " + t.getMessage(), t);
        }

        return (ngrams == null)
                ? Collections.emptySortedSet()
                : ngrams;
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
        private static final Map<String, String> IRREGULARS;

        static {
            Map<String, String> map = new HashMap<>();

            // ==========================================
            // IRREGULAR VERBS (Past / Participle / 3rd Person -> Infinitive)
            // ==========================================
            // Be
            map.put("was", "be");
            map.put("were", "be");
            map.put("am", "be");
            map.put("is", "be");
            map.put("are", "be");
            map.put("been", "be");
            // Have
            map.put("had", "have");
            map.put("has", "have");
            // Do
            map.put("did", "do");
            map.put("does", "do");
            map.put("done", "do");
            // Go
            map.put("went", "go");
            map.put("gone", "go");
            map.put("goes", "go");
            // Common irregulars
            map.put("ran", "run");
            map.put("runs", "run");
            map.put("ate", "eat");
            map.put("eaten", "eat");
            map.put("eats", "eat");
            map.put("saw", "see");
            map.put("seen", "see");
            map.put("sees", "see");
            map.put("came", "come");
            map.put("comes", "come");
            map.put("took", "take");
            map.put("taken", "take");
            map.put("takes", "take");
            map.put("made", "make");
            map.put("makes", "make");
            map.put("gave", "give");
            map.put("given", "give");
            map.put("gives", "give");
            map.put("knew", "know");
            map.put("known", "know");
            map.put("knows", "know");
            map.put("got", "get");
            map.put("gotten", "get");
            map.put("gets", "get");
            map.put("found", "find");
            map.put("finds", "find");
            map.put("thought", "think");
            map.put("thinks", "think");
            map.put("told", "tell");
            map.put("tells", "tell");
            map.put("became", "become");
            map.put("becomes", "become");
            map.put("left", "leave");
            map.put("leaves", "leave"); // "leaves" as noun handled below
            map.put("felt", "feel");
            map.put("feels", "feel");
            map.put("brought", "bring");
            map.put("brings", "bring");
            map.put("began", "begin");
            map.put("begun", "begin");
            map.put("begins", "begin");
            map.put("kept", "keep");
            map.put("keeps", "keep");
            map.put("held", "hold");
            map.put("holds", "hold");
            map.put("wrote", "write");
            map.put("written", "write");
            map.put("writes", "write");
            map.put("stood", "stand");
            map.put("stands", "stand");
            map.put("heard", "hear");
            map.put("hears", "hear");
            map.put("meant", "mean");
            map.put("means", "mean");
            map.put("set", "set");
            map.put("sets", "set");
            map.put("met", "meet");
            map.put("meets", "meet");
            map.put("paid", "pay");
            map.put("pays", "pay");
            map.put("sat", "sit");
            map.put("sits", "sit");
            map.put("spoke", "speak");
            map.put("spoken", "speak");
            map.put("speaks", "speak");
            map.put("led", "lead");
            map.put("leads", "lead"); // "lead" as noun handled below
            map.put("read", "read");
            map.put("reads", "read");
            map.put("grew", "grow");
            map.put("grown", "grow");
            map.put("grows", "grow");
            map.put("lost", "lose");
            map.put("loses", "lose");
            map.put("fell", "fall");
            map.put("fallen", "fall");
            map.put("falls", "fall");
            map.put("sent", "send");
            map.put("sends", "send");
            map.put("built", "build");
            map.put("builds", "build");
            map.put("understood", "understand");
            map.put("understands", "understand");
            map.put("cut", "cut");
            map.put("cuts", "cut");
            map.put("put", "put");
            map.put("puts", "put");
            map.put("hit", "hit");
            map.put("hits", "hit");
            map.put("bought", "buy");
            map.put("buys", "buy");
            map.put("caught", "catch");
            map.put("catches", "catch");
            map.put("drew", "draw");
            map.put("drawn", "draw");
            map.put("draws", "draw");
            map.put("drove", "drive");
            map.put("driven", "drive");
            map.put("drives", "drive");
            map.put("broke", "break");
            map.put("broken", "break");
            map.put("breaks", "break");
            map.put("chose", "choose");
            map.put("chosen", "choose");
            map.put("chooses", "choose");
            map.put("drank", "drink");
            map.put("drunk", "drink");
            map.put("drinks", "drink");
            map.put("flew", "fly");
            map.put("flown", "fly");
            map.put("flies", "fly");
            map.put("swam", "swim");
            map.put("swum", "swim");
            map.put("swims", "swim");
            map.put("rang", "ring");
            map.put("rung", "ring");
            map.put("rings", "ring");
            map.put("sang", "sing");
            map.put("sung", "sing");
            map.put("sings", "sing");
            map.put("sank", "sink");
            map.put("sunk", "sink");
            map.put("sinks", "sink");
            map.put("shook", "shake");
            map.put("shaken", "shake");
            map.put("shakes", "shake");
            map.put("stole", "steal");
            map.put("stolen", "steal");
            map.put("steals", "steal");
            map.put("swore", "swear");
            map.put("sworn", "swear");
            map.put("swears", "swear");
            map.put("threw", "throw");
            map.put("thrown", "throw");
            map.put("throws", "throw");
            map.put("wore", "wear");
            map.put("worn", "wear");
            map.put("wears", "wear");
            map.put("bit", "bite");
            map.put("bitten", "bite");
            map.put("bites", "bite");
            map.put("hid", "hide");
            map.put("hidden", "hide");
            map.put("hides", "hide");
            map.put("froze", "freeze");
            map.put("frozen", "freeze");
            map.put("freezes", "freeze");
            map.put("rose", "rise");
            map.put("risen", "rise");
            map.put("rises", "rise");
            map.put("woke", "wake");
            map.put("woken", "wake");
            map.put("wakes", "wake");
            map.put("wove", "weave");
            map.put("woven", "weave");
            map.put("weaves", "weave");
            map.put("tore", "tear");
            map.put("torn", "tear");
            map.put("tears", "tear");
            map.put("shrank", "shrink");
            map.put("shrunk", "shrink");
            map.put("shrinks", "shrink");
            map.put("struck", "strike");
            map.put("strikes", "strike");
            map.put("sought", "seek");
            map.put("seeks", "seek");
            map.put("fought", "fight");
            map.put("fights", "fight");
            map.put("bound", "bind");
            map.put("binds", "bind");
            map.put("ground", "grind");
            map.put("grinds", "grind");
            map.put("wound", "wind");
            map.put("winds", "wind");
            map.put("spun", "spin");
            map.put("spins", "spin");
            map.put("clung", "cling");
            map.put("clings", "cling");
            map.put("stung", "sting");
            map.put("stings", "sting");

            map.put("swung", "swing");
            map.put("swings", "swing");
            map.put("wrung", "wring");
            map.put("wrings", "wring");
            map.put("slung", "sling");
            map.put("slings", "sling");
            map.put("stuck", "stick");
            map.put("sticks", "stick");
            map.put("dealt", "deal");
            map.put("deals", "deal");
            map.put("knelt", "kneel");
            map.put("kneels", "kneel");
            map.put("leant", "lean");
            map.put("leans", "lean");
            map.put("leapt", "leap");
            map.put("leaps", "leap");
            map.put("crept", "creep");
            map.put("creeps", "creep");
            map.put("wept", "weep");
            map.put("weeps", "weep");
            map.put("slept", "sleep");
            map.put("sleeps", "sleep");
            map.put("swept", "sweep");
            map.put("sweeps", "sweep");
            map.put("fed", "feed");
            map.put("feeds", "feed");
            map.put("bred", "breed");
            map.put("breeds", "breed");
            map.put("bled", "bleed");
            map.put("bleeds", "bleed");
            map.put("fled", "flee");
            map.put("flees", "flee");
            map.put("sped", "speed");
            map.put("speeds", "speed");
            map.put("shed", "shed");
            map.put("sheds", "shed");
            map.put("spread", "spread");
            map.put("spreads", "spread");
            map.put("bet", "bet");
            map.put("bets", "bet");
            map.put("cast", "cast");
            map.put("casts", "cast");
            map.put("cost", "cost");
            map.put("costs", "cost");
            map.put("shut", "shut");
            map.put("shuts", "shut");
            map.put("split", "split");
            map.put("splits", "split");
            map.put("let", "let");
            map.put("lets", "let");
            map.put("burst", "burst");
            map.put("bursts", "burst");
            map.put("hung", "hang");
            map.put("hangs", "hang");
            map.put("spat", "spit");
            map.put("spits", "spit");
            map.put("lit", "light");
            map.put("lights", "light");
            map.put("bid", "bid");
            map.put("bids", "bid");

            // ==========================================
            // IRREGULAR NOUNS (Plural -> Singular)
            // ==========================================
            // Vowel changes / Old English
            map.put("men", "man");
            map.put("women", "woman");
            map.put("children", "child");
            map.put("oxen", "ox");
            map.put("feet", "foot");
            map.put("geese", "goose");
            map.put("teeth", "tooth");
            map.put("mice", "mouse");
            map.put("lice", "louse");
            map.put("brethren", "brother");

            // Latin/Greek origin plurals (Common in technical/academic search)
            map.put("analyses", "analysis");
            map.put("bases", "base"); // Also plural of basis, but base is a safer lemma for search
            map.put("crises", "crisis");
            map.put("diagnoses", "diagnosis");
            map.put("hypotheses", "hypothesis");
            map.put("oases", "oasis");
            map.put("parentheses", "parenthesis");
            map.put("theses", "thesis");
            map.put("axes", "axis");
            map.put("phenomena", "phenomenon");
            map.put("criteria", "criterion");
            map.put("data", "datum"); // Often treated as mass noun, but mathematically correct
            map.put("media", "medium");
            map.put("bacteria", "bacterium");
            map.put("curricula", "curriculum");
            map.put("memoranda", "memorandum");
            map.put("strata", "stratum");
            map.put("alumni", "alumnus");
            map.put("cacti", "cactus"); // cactuses also valid, but cacti common
            map.put("foci", "focus");
            map.put("fungi", "fungus");
            map.put("nuclei", "nucleus");
            map.put("radii", "radius");
            map.put("stimuli", "stimulus");
            map.put("syllabi", "syllabus");
            map.put("appendices", "appendix");
            map.put("indices", "index"); // indexes also valid
            map.put("matrices", "matrix");
            map.put("vertices", "vertex");
            map.put("bureaux", "bureau"); // bureaus also valid
            map.put("plateaux", "plateau");
            map.put("tableaux", "tableau");

            // Unchanging / Zero Plurals (Search indexes benefit from explicit mapping here)
            map.put("sheep", "sheep");
            map.put("deer", "deer");
            map.put("fish", "fish"); // fishes exists for species, but fish is primary
            map.put("species", "species");
            map.put("series", "series");
            map.put("aircraft", "aircraft");
            map.put("moose", "moose");
            map.put("swine", "swine");

            // ==========================================
            // IRREGULAR ADJECTIVES (Comparative/Superlative -> Base)
            // ==========================================
            map.put("better", "good");
            map.put("best", "good");
            map.put("worse", "bad");
            map.put("worst", "bad");
            map.put("less", "little");
            map.put("least", "little");
            map.put("further", "far");
            map.put("farthest", "far");
            map.put("furthest", "far");
            map.put("elder", "old");
            map.put("eldest", "old");
            map.put("more", "much");
            map.put("most", "much");

            IRREGULARS = Map.copyOf(map);
        }

        /**
         * Checks for common stop words, such as 'the', 'a'.
         *
         * @param word must be non-blank in lowercase already, for speed reasons
         * @return {@code true} if the word is a stop word
         */
        static boolean isStopWord(String word) {
            return EnglishMorphMethods.STOP_WORDS.contains(word);
        }

        /**
         * Converts English words to their base forms, taking into account some (but not all) irregular words. The
         * common stop words are not filtered out, rathe processed, such as 'was' becomes 'be'.
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
