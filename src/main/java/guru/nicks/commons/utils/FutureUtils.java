package guru.nicks.commons.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Future-related utility methods.
 */
@UtilityClass
@Slf4j
public class FutureUtils {

    /**
     * The number of virtual threads is unbounded, so sharing the executor everywhere is the intended usage pattern. To
     * such methods as {@link CompletableFuture#thenAccept(Consumer)}, pass {@link #captureMdcForChildThreads()} in
     * order to inherit (and therefore log) parent thread's {@link MDC}.
     *
     * @see #captureMdcForChildThreads()
     */
    public static final Executor VIRTUAL_THREAD_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Same as {@link #getInParallel(Collection, int)}, just the batch size equals the number of tasks.
     *
     * @param tasks tasks to run
     * @return results of completed futures - in the same order as in the input collection
     */
    public static <T> List<T> getInParallel(Collection<Supplier<T>> tasks) {
        return FutureUtils.getInParallel(tasks, tasks.size());
    }

    /**
     * Creates {@link CompletableFuture} for each {@link Supplier}, runs them in parallel (no more than
     * {@code maxConcurrentTasks} at a time), and awaits their completion. The point is to limit not the number of
     * threads (virtual threads scale up to millions easily), but the number of memory- and/or IO-heavy workers.
     *
     * @param tasks              tasks to run
     * @param maxConcurrentTasks max. number of concurrent (parallel) tasks
     * @param <T>                future result type
     * @return results of completed futures - in the same order as in the input collection
     * @throws IllegalArgumentException the task limit is not positive (only if the task list is not empty)
     */
    public static <T> List<T> getInParallel(Collection<Supplier<T>> tasks, int maxConcurrentTasks) {
        // early return for speedup
        if (CollectionUtils.isEmpty(tasks)) {
            return List.of();
        }

        if (maxConcurrentTasks <= 0) {
            throw new IllegalArgumentException("Max. number of concurrent tasks should be positive");
        }

        // in Java 17, there is 'newFixedThreadPool(limit)', but for virtual threads, no thread limiting is available,
        // hence a semaphore for concurrency limiting
        var semaphore = new Semaphore(maxConcurrentTasks);
        var executor = captureMdcForChildThreads();
        var i = new AtomicInteger();

        List<CompletableFuture<T>> futures = tasks.stream()
                .filter(Objects::nonNull)
                .map(task -> {
                    // acquire permit BEFORE creating the future
                    try {
                        semaphore.acquire();
                    }
                    // WARNING: this means already submitted tasks will complete, but their results will be lost
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Thread interrupted while acquiring semaphore permit: "
                                + e.getMessage(), e);
                    }

                    int taskNumber = i.incrementAndGet();

                    // In a very unlikely event when supplyAsync() fails during task submission, no future is created,
                    // and whenComplete() isn't called, so the semaphore is never released. But this doesn't require
                    // handling, as the outer loop fails and the exception propagates to caller.
                    return CompletableFuture
                            .supplyAsync(task, executor)
                            .whenComplete((result, throwable) -> {
                                if (throwable != null) {
                                    log.error("Task {}/{} failed: {}", taskNumber, tasks.size(),
                                            throwable.getMessage(), throwable);
                                }

                                semaphore.release();
                            });
                })
                .toList();

        CompletableFuture
                .allOf(futures.toArray(CompletableFuture[]::new))
                .join();

        // collect results in the order of future creation (caller may depend on that)
        return TransformUtils.toList(futures, CompletableFuture::join);
    }

    /**
     * Same as {@link #runInParallel(Collection, int)}, just the batch size equals the number of tasks.
     *
     * @param tasks tasks to run
     */
    public static void runInParallel(Collection<? extends Runnable> tasks) {
        runInParallel(tasks, tasks.size());
    }

    /**
     * Same as {@link #getInParallel(Collection, int)}, just no results are returned.
     *
     * @param tasks              tasks to run
     * @param maxConcurrentTasks max. number of concurrent (parallel) tasks
     */
    public static void runInParallel(Collection<? extends Runnable> tasks, int maxConcurrentTasks) {
        List<Supplier<Void>> suppliers = tasks.stream()
                .map(TransformUtils::toSupplier)
                .toList();
        getInParallel(suppliers, maxConcurrentTasks);
    }

    /**
     * Wraps {@link #VIRTUAL_THREAD_EXECUTOR} executor to automatically propagate a snapshot of caller thread's
     * {@link MDC} context to child threads and clear it upon task completion. The context variables can be logged using
     * the {@code %X} placeholder.
     * <p>
     * The MDC <b>context is captured only once</b> - when this method is called.
     * <p>
     * Usage example:
     * <pre>
     *  // inherit MDC from this thread
     *  var executor = FutureUtils.captureMdcForChildThreads();
     *  // pass MDC to new threads
     *  var future = CompletableFuture.supplyAsync(() -> someService.someMethod(), executor);
     * </pre>
     *
     * @return executor
     */
    public static Executor captureMdcForChildThreads() {
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
