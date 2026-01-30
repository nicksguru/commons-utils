package guru.nicks.commons.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Future-related utility methods.
 */
@UtilityClass
public class FutureUtils {

    /**
     * The number of virtual threads is unbounded, so sharing the executor everywhere is the intended usage pattern. To
     * such methods as {@link CompletableFuture#thenAccept(Consumer)}, pass {@link #createMdcAwareExecutor()} in order
     * to inherit (and therefore output in logs) parent thread's {@link MDC}.
     *
     * @see #createMdcAwareExecutor()
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
     * {@code limit} virtual threads at a time), and awaits their completion. The point is to limit not the number of
     * threads (virtual threads scale up to millions normally), but the number of memory- and/or IO-heavy workers.
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

        // in Java 17, there is 'newFixedThreadPool(limit)', but for virtual threads, no thread limiting is available,
        // hence a semaphore for concurrency limiting
        var semaphore = new Semaphore(limit, true);
        var executor = createMdcAwareExecutor();

        List<CompletableFuture<T>> futures = tasks.stream()
                .filter(Objects::nonNull)
                .map(task -> {
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
                            return task.get();
                        } finally {
                            semaphore.release();
                        }
                    }, executor);
                })
                .toList();

        CompletableFuture
                .allOf(futures.toArray(CompletableFuture[]::new))
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
     * Wraps {@link #VIRTUAL_THREAD_EXECUTOR} executor to automatically propagate {@link MDC} context from caller thread
     * to child threads. This ensures that all tasks will have access to the MDC context that was present when the task
     * was submitted. The context variables (such as current user ID, request ID, etc.) can be output in logs using the
     * {@code %X} placeholder.
     * <p>
     * The MDC <b>context is captured only once</b> - when this method is called, from the caller's thread. Later on,
     * it's passed to all tasks.
     * <p>
     * Usage example:
     * <pre>
     *  // inherit MDC from this thread
     *  var executor = FutureUtils.createMdcAwareExecutor();
     *  // pass MDC to new threads
     *  var future = CompletableFuture.supplyAsync(() -> someService.someMethod(), executor);
     * </pre>
     *
     * @return executor
     */
    public static Executor createMdcAwareExecutor() {
        var parentMdc = MDC.getCopyOfContextMap();

        return (Runnable task) -> VIRTUAL_THREAD_EXECUTOR.execute(() -> {
            try {
                // SLF4j should clear MDC if null is passed, but this isn't guaranteed across all implementations,
                // hence manual clearing
                if (parentMdc != null) {
                    MDC.setContextMap(parentMdc);
                } else {
                    MDC.clear();
                }

                task.run();
            } finally {
                MDC.clear();
            }
        });
    }

}
