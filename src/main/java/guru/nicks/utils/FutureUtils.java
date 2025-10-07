package guru.nicks.utils;

import lombok.experimental.UtilityClass;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * Future-related utility methods.
 */
@UtilityClass
public class FutureUtils {

    /**
     * Same as {@link #getInParallel(Collection, int)}, just the number of threads equals the number of tasks.
     *
     * @param code code to run
     * @return results of completed futures - in the same order as in the input collection
     */
    public static <T> List<T> getInParallel(Collection<Supplier<T>> code) {
        return FutureUtils.getInParallel(code, code.size());
    }

    /**
     * Creates {@link CompletableFuture} for each {@link Supplier}, runs them in parallel (with no more than
     * {@code limit} virtual threads at a time), and awaits their completion.
     *
     * @param code  code to run
     * @param limit max. number of threads
     * @param <T>   future result type
     * @return results of completed futures - in the same order as in the input collection
     */
    public static <T> List<T> getInParallel(Collection<Supplier<T>> code, int limit) {
        var parentMdc = MDC.getCopyOfContextMap();

        // in Java 17, there is 'newFixedThreadPool(limit)', but for virtual threads, no thread limiting is available,
        // hence a semaphore for concurrency limiting
        var semaphore = new Semaphore(limit, true);

        try (var threadPool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<T>> futures = code.stream()
                    .map(supplier -> CompletableFuture.supplyAsync(() -> {
                        try {
                            // apply task count limit
                            semaphore.acquire();
                            // inherit MDC from parent thread, otherwise it becomes empty
                            MDC.setContextMap(parentMdc);

                            return supplier.get();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Thread interrupted while acquiring semaphore permit: "
                                    + e.getMessage(), e);
                        } finally {
                            MDC.clear();
                            // let other threads acquire this permit
                            semaphore.release();
                        }
                    }, threadPool))
                    .toList();

            CompletableFuture
                    .allOf(futures.toArray(new CompletableFuture[0]))
                    .join();

            // collect results in the order of future creation (caller may depend on that)
            return TransformUtils.toList(futures, CompletableFuture::join);
        }
    }

    /**
     * Same as {@link #runInParallel(Collection, int)}, just the number of threads equals the number of tasks.
     *
     * @param code code to run
     */
    public static void runInParallel(Collection<? extends Runnable> code) {
        runInParallel(code, code.size());
    }

    /**
     * Creates {@link CompletableFuture} for each {@link Runnable}, runs them in parallel (but with no more than
     * {@code limit} threads), and awaits their completion.
     *
     * @param code  code to run
     * @param limit max. number of threads
     */
    public static void runInParallel(Collection<? extends Runnable> code, int limit) {
        List<Supplier<Void>> suppliers = code.stream()
                .map(TransformUtils::toSupplier)
                .toList();
        getInParallel(suppliers, limit);
    }

}
