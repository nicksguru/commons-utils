package guru.nicks.commons.utils.text;

import com.github.demidko.aot.WordformMeaning;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Russian morphology utility methods for lemmatization using the AOT library.
 * <p>
 * This class provides lazy-loaded access to method handles for the optional {@code com.github.demidko:aot} library. If
 * the library is not available on the classpath, all methods will gracefully return {@code null}.
 */
@UtilityClass
@Slf4j
public class RussianUtils {

    /**
     * Cached method handles for Russian morphological analysis, or {@code null} if not available. Uses
     * {@link AtomicReference} for thread-safe lazy initialization.
     */
    private static boolean initializedOrFailed;

    /**
     * Method handle for {@code WordformMeaning.lookupForMeanings(String)}.
     */
    private static MethodHandle lookupForMeaningsMethod;

    /**
     * Method handle for {@code WordformMeaning.getLemma()}.
     */
    private static MethodHandle getLemmaMethod;

    /**
     * Converts the Russian word to its base form, taking into irregular forms, such as 'люди' → 'человек'.
     *
     * @param word will be converted to lowercase, and leading/trailing whitespaces removed
     * @return lemma, or the original word if it wasn't recognized as a Russian word (e.g. has punctuation characters or
     *         belongs to another language)
     * @throws IllegalStateException analysis not available
     */
    public static String getWordLemma(String word) {
        initializeMethodHandlesOnce();

        if (getLemmaMethod == null) {
            throw new IllegalStateException("Failed to initialize morphology methods");
        }

        if (StringUtils.isBlank(word)) {
            return word;
        }

        try {
            word = word.strip().toLowerCase();
            List<WordformMeaning> meanings = findWordMeanings(word);

            if (meanings.isEmpty()) {
                return word;
            }

            // lemmatize the first meaning
            WordformMeaning lemmaMeaning = (WordformMeaning) getLemmaMethod.invoke(meanings.getFirst());
            return lemmaMeaning.toString();
        } catch (Throwable t) {
            throw new IllegalStateException("Morphological analysis failed: " + t.getMessage(), t);
        }
    }

    /**
     * Looks up word meanings. Returns an empty list for unknown words, for example for any non-Russian ones.
     *
     * @param word will be converted to lowercase, and leading/trailing whitespaces removed
     * @return morphological meanings of the word
     * @throws IllegalStateException analysis not available
     */
    public static List<WordformMeaning> findWordMeanings(String word) {
        initializeMethodHandlesOnce();

        if (lookupForMeaningsMethod == null) {
            throw new IllegalStateException("Failed to initialize morphology methods");
        }

        if (StringUtils.isBlank(word)) {
            return List.of();
        }

        word = word.strip().toLowerCase();

        try {
            return (List<WordformMeaning>) lookupForMeaningsMethod.invoke(word);
        } catch (Throwable t) {
            throw new IllegalStateException("Morphological analysis failed: " + t.getMessage(), t);
        }
    }

    /**
     * Attempts to initialize morphology handles (once) by loading the class and creating method handles.
     *
     * @throws IllegalStateException initialization failed (the exact cause is wrapped in this exception)
     */
    private static void initializeMethodHandlesOnce() {
        if (initializedOrFailed) {
            return;
        }

        synchronized (RussianUtils.class) {
            if (initializedOrFailed) {
                return;
            }

            try {
                Class<?> clazz = Class.forName("com.github.demidko.aot.WordformMeaning");
                MethodHandles.Lookup lookup = MethodHandles.lookup();

                // create method handle for static method: 'List WordformMeaning.lookupForMeanings(String)'
                lookupForMeaningsMethod = lookup.findStatic(clazz, "lookupForMeanings",
                        MethodType.methodType(List.class, String.class));

                // create method handle for virtual method: 'WordformMeaning getLemma()'
                getLemmaMethod = lookup.findVirtual(clazz, "getLemma", MethodType.methodType(clazz));
                initializedOrFailed = true;
            } catch (Exception e) {
                initializedOrFailed = true;
                throw new IllegalStateException("Failed to initialize morphology methods: " + e.getMessage(), e);
            }
        }
    }

}
