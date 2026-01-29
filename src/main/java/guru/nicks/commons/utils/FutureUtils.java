package guru.nicks.commons.utils;

import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Future-related utility methods.
 */
@UtilityClass
public class FutureUtils {

    /**
     * The number of virtual threads is unbounded, so sharing the executor everywhere is the intended usage pattern.
     */
    public static final Executor VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Same as {@link #getInParallel(Collection, int)}, just the number of threads equals the number of tasks.
     *
     * @param tasks tasks to run
     * @return results of completed futures - in the same order as in the input collection
     */
    public static <T> List<T> getInParallel(Collection<Supplier<T>> tasks) {
        return FutureUtils.getInParallel(tasks, tasks.size());
    }

    /**
     * Creates {@link CompletableFuture} for each {@link Supplier}, runs them in parallel (with no more than
     * {@code limit} virtual threads at a time), and awaits their completion.
     *
     * @param tasks tasks to run
     * @param limit max. number of threads
     * @param <T>   future result type
     * @return results of completed futures - in the same order as in the input collection
     * @throws IllegalArgumentException the task limit is not positive (only if the task list is not empty)
     */
    public static <T> List<T> getInParallel(Collection<Supplier<T>> tasks, int limit) {
        // early return for speedup
        if (CollectionUtils.isEmpty(tasks)) {
            return List.of();
        }

        if (limit <= 0) {
            throw new IllegalArgumentException("Thread limit should be positive");
        }

        var parentMdc = MDC.getCopyOfContextMap();

        // in Java 17, there is 'newFixedThreadPool(limit)', but for virtual threads, no thread limiting is available,
        // hence a semaphore for concurrency limiting
        var semaphore = new Semaphore(limit, true);

        List<CompletableFuture<T>> futures = tasks.stream()
                .filter(Objects::nonNull)
                .map(supplier -> {
                    // acquire permit BEFORE creating the future - to actually limit concurrent task submissions
                    try {
                        semaphore.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Thread interrupted while acquiring semaphore permit: "
                                + e.getMessage(), e);
                    }

                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            return withMdc(parentMdc, supplier).get();
                        } finally {
                            semaphore.release();
                        }
                    }, VIRTUAL_THREAD_EXECUTOR);
                })
                .toList();

        CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .join();

        // collect results in the order of future creation (caller may depend on that)
        return TransformUtils.toList(futures, CompletableFuture::join);
    }

    /**
     * Same as {@link #runInParallel(Collection, int)}, just the number of threads equals the number of tasks.
     *
     * @param tasks tasks to run
     */
    public static void runInParallel(Collection<? extends Runnable> tasks) {
        runInParallel(tasks, tasks.size());
    }

    /**
     * Creates {@link CompletableFuture} for each {@link Runnable}, runs them in parallel (but with no more than
     * {@code limit} virtual threads at a time), and awaits their completion.
     *
     * @param tasks tasks to run
     * @param limit max. number of threads
     */
    public static void runInParallel(Collection<? extends Runnable> tasks, int limit) {
        List<Supplier<Void>> suppliers = tasks.stream()
                .map(TransformUtils::toSupplier)
                .toList();
        getInParallel(suppliers, limit);
    }

    /**
     * Wraps a function with {@link MDC} context propagation from parent thread. Without this, logging from a new thread
     * will have an empty MDC.
     *
     * @param parentMdc parent thread's MDC context (can be {@code null})
     * @param task      task to execute
     * @param <T>       task result type
     * @return function that sets MDC before task execution and clears it after task execution
     */
    public <T> Supplier<T> withMdc(@Nullable Map<String, String> parentMdc, Supplier<T> task) {
        return () -> {
            // set or clear MDC
            MDC.setContextMap(parentMdc);

            try {
                return task.get();
            } finally {
                MDC.clear();
            }
        };
    }

    /**
     * Does the same as {@link #withMdc(Map, Supplier)}, just the argument is a {@link BiFunction}. Useful for
     * {@link CompletableFuture#thenCombineAsync(CompletionStage, BiFunction)}.
     *
     * @param parentMdc parent thread's MDC context (can be {@code null})
     * @param task      task to execute
     * @param <T>       task result type
     * @param <U>       first argument type
     * @param <R>       second argument type
     * @return function that sets MDC before task execution and clears it after task execution
     */
    public <T, U, R> BiFunction<T, U, R> withMdc(@Nullable Map<String, String> parentMdc, BiFunction<T, U, R> task) {
        return (arg1, arg2) -> {
            // set or clear MDC
            MDC.setContextMap(parentMdc);

            try {
                return task.apply(arg1, arg2);
            } finally {
                MDC.clear();
            }
        };
    }

}
