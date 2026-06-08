package guru.nicks.commons.utils.text;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

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
    private static MethodHandle lookupForMeanings;

    /**
     * Method handle for {@code WordformMeaning.getLemma()}.
     */
    private static MethodHandle getLemma;

    /**
     * Gets the method handle for {@code WordformMeaning.lookupForMeanings(String)}. The method call returns an empty
     * list for unknown words, for example for any non-Russian ones.
     *
     * @return method handle
     * @throws IllegalStateException method not available
     */
    public static MethodHandle getLookupForMeaningsMethod() {
        initializeMethodHandlesOnce();

        if (lookupForMeanings == null) {
            throw new IllegalStateException("Failed to initialize morphology handles");
        }

        return lookupForMeanings;
    }

    /**
     * Gets the method handle for {@code WordformMeaning.getLemma()}.
     *
     * @return method handle
     * @throws IllegalStateException method not available
     */
    public static MethodHandle getGetLemmaMethod() {
        initializeMethodHandlesOnce();

        if (getLemma == null) {
            throw new IllegalStateException("Failed to initialize morphology handles");
        }

        return getLemma;
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
                Class<?> wordformMeaningClass = Class.forName("com.github.demidko.aot.WordformMeaning");
                var lookup = MethodHandles.lookup();

                // create method handle for static method: 'List WordformMeaning.lookupForMeanings(String)'
                lookupForMeanings = lookup.findStatic(wordformMeaningClass, "lookupForMeanings",
                        MethodType.methodType(List.class, String.class));

                // create method handle for virtual method: 'WordformMeaning getLemma()'
                getLemma = lookup.findVirtual(wordformMeaningClass, "getLemma",
                        MethodType.methodType(Class.forName("com.github.demidko.aot.WordformMeaning")));
                initializedOrFailed = true;

            } catch (Exception e) {
                initializedOrFailed = true;
                throw new IllegalStateException("Failed to initialize Russian morphology handles: " + e.getMessage(),
                        e);
            }
        }
    }

}
