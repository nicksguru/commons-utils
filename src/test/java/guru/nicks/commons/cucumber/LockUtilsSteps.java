package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.LockUtils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for testing {@link LockUtils} functionality.
 */
@RequiredArgsConstructor
public class LockUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private final AtomicInteger result = new AtomicInteger(0);
    private final AtomicBoolean optimisticReadRetried = new AtomicBoolean(false);
    private final AtomicBoolean writeLockAcquired = new AtomicBoolean(false);
    private final List<Integer> threadResults = new ArrayList<>();

    int threadCount;
    private StampedLock lock;
    private CountDownLatch writeLockHeldLatch;
    private CountDownLatch readStartedLatch;
    private CountDownLatch completionLatch;

    @Given("a StampedLock instance")
    public void aStampedLockInstance() {
        lock = new StampedLock();
        optimisticReadRetried.set(false);
        writeLockAcquired.set(false);
        threadResults.clear();
    }

    @Given("a null StampedLock instance")
    public void aNullStampedLockInstance() {
        lock = null;
    }

    @Given("a background thread holding a write lock")
    public void aBackgroundThreadHoldingAWriteLock() {
        writeLockHeldLatch = new CountDownLatch(1);
        readStartedLatch = new CountDownLatch(1);

        // wait for the main thread to start the read operation
        // hold the lock for a short time to ensure contention
        var backgroundThread = new Thread(() -> {
            long stamp = lock.writeLock();

            try {
                writeLockAcquired.set(true);
                writeLockHeldLatch.countDown();

                // wait for the main thread to start the read operation
                try {
                    readStartedLatch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // hold the lock for a short time to ensure contention
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                lock.unlock(stamp);
            }
        });

        backgroundThread.start();

        // wait for the background thread to acquire the write lock
        try {
            boolean acquired = writeLockHeldLatch.await(5, TimeUnit.SECONDS);

            assertThat(acquired)
                    .as("write lock acquired")
                    .isTrue();

            assertThat(writeLockAcquired.get())
                    .as("writeLockAcquired")
                    .isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @When("the optimistic read lock is used with no write contention")
    public void theOptimisticReadLockIsUsedWithNoWriteContention() {
        // create custom implementation to track if retry happens
        StampedLock trackedLock = new StampedLock() {
            @Override
            public boolean validate(long stamp) {
                boolean valid = super.validate(stamp);

                if (!valid) {
                    optimisticReadRetried.set(true);
                }

                return valid;
            }
        };

        textWorld.setLastException(catchThrowable(() ->
                result.set(LockUtils.withOptimisticReadOrRetry(trackedLock, () -> 42))));
    }

    @When("the optimistic read lock is used with write contention")
    public void theOptimisticReadLockIsUsedWithWriteContention() {
        // create custom implementation to track if retry happens
        StampedLock trackedLock = new StampedLock() {
            @Override
            public boolean validate(long stamp) {
                boolean valid = super.validate(stamp);

                if (!valid) {
                    optimisticReadRetried.set(true);
                }

                return valid;
            }
        };

        // Acquire a write lock in a background thread
        Thread writerThread = new Thread(() -> {
            long stamp = trackedLock.writeLock();

            try {
                // hold the lock briefly
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                trackedLock.unlock(stamp);
            }
        });

        writerThread.start();

        // give the writer thread time to acquire the lock
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        textWorld.setLastException(catchThrowable(() ->
                result.set(LockUtils.withOptimisticReadOrRetry(trackedLock, () -> 42))));

        try {
            writerThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @When("the exclusive write lock is used")
    public void theExclusiveWriteLockIsUsed() {
        textWorld.setLastException(catchThrowable(() ->
                result.set(LockUtils.withExclusiveLock(lock, () -> {
                    writeLockAcquired.set(true);
                    return 42;
                }))));
    }

    @When("multiple threads use optimistic read concurrently")
    public void multipleThreadsUseOptimisticReadConcurrently() {
        threadCount = 10;
        completionLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        textWorld.setLastException(catchThrowable(() -> {
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;

                executor.submit(() -> {
                    try {
                        int value = LockUtils.withOptimisticReadOrRetry(lock, () -> threadId);

                        synchronized (threadResults) {
                            threadResults.add(value);
                        }
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            boolean completed = completionLatch.await(5, TimeUnit.SECONDS);
            assertThat(completed).as("all threads completed").isTrue();

            executor.shutdown();
            boolean terminated = executor.awaitTermination(1, TimeUnit.SECONDS);
            assertThat(terminated).as("executor terminated").isTrue();
        }));
    }

    @When("multiple threads compete for the write lock")
    public void multipleThreadsCompeteForTheWriteLock() {
        threadCount = 5;
        completionLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        textWorld.setLastException(catchThrowable(() -> {
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;

                executor.submit(() -> {
                    try {
                        int value = LockUtils.withExclusiveLock(lock, () -> {
                            // simulate some work
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }

                            return threadId;
                        });

                        synchronized (threadResults) {
                            threadResults.add(value);
                        }
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            boolean completed = completionLatch.await(5, TimeUnit.SECONDS);
            assertThat(completed).as("all threads completed").isTrue();

            executor.shutdown();
            boolean terminated = executor.awaitTermination(1, TimeUnit.SECONDS);
            assertThat(terminated).as("executor terminated").isTrue();
        }));
    }

    @When("the optimistic read lock is used")
    public void theOptimisticReadLockIsUsed() {
        textWorld.setLastException(catchThrowable(() ->
                LockUtils.withOptimisticReadOrRetry(lock, () -> 42)));
    }

    @When("the optimistic read lock is used with null code")
    public void theOptimisticReadLockIsUsedWithNullCode() {
        textWorld.setLastException(catchThrowable(() ->
                LockUtils.withOptimisticReadOrRetry(lock, (Supplier<Integer>) null)));
    }

    @Then("the operation should complete successfully")
    public void theOperationShouldCompleteSuccessfully() {
        assertThat(textWorld.getLastException()).as("lastException").isNull();
        assertThat(result.get()).as("result").isEqualTo(42);
    }

    @Then("the optimistic read should not be retried")
    public void theOptimisticReadShouldNotBeRetried() {
        assertThat(optimisticReadRetried.get()).as("optimisticReadRetried").isFalse();
    }

    @Then("the optimistic read should be retried with a real read lock")
    public void theOptimisticReadShouldBeRetriedWithARealReadLock() {
        assertThat(optimisticReadRetried.get()).as("optimisticReadRetried").isTrue();
    }

    @Then("the write lock should be released after completion")
    public void theWriteLockShouldBeReleasedAfterCompletion() {
        assertThat(writeLockAcquired.get()).as("writeLockAcquired").isTrue();

        // verify the lock was released by trying to acquire it again
        long stamp = lock.tryWriteLock();
        assertThat(stamp).as("write lock stamp").isNotZero();

        if (stamp != 0) {
            lock.unlock(stamp);
        }
    }

    @Then("all operations should complete successfully")
    public void allOperationsShouldCompleteSuccessfully() {
        assertThat(textWorld.getLastException()).as("lastException").isNull();
        assertThat(threadResults).as("threadResults").hasSize(
                completionLatch.getCount() == 0
                        ? threadCount
                        : 0);
    }

    @Then("no thread synchronization issues should occur")
    public void noThreadSynchronizationIssuesShouldOccur() {
        // This is verified by the absence of exceptions and the correct count of results
        assertThat(textWorld.getLastException()).as("lastException").isNull();
    }

    @Then("each thread should get exclusive access")
    public void eachThreadShouldGetExclusiveAccess() {
        // If all threads completed without exceptions, they successfully acquired exclusive access
        assertThat(textWorld.getLastException()).as("lastException").isNull();

        // verify all expected thread IDs are in the results
        List<Integer> expectedIds = IntStream.range(0, threadCount).boxed().toList();

        assertThat(threadResults).as("threadResults")
                .hasSize(threadCount)
                .containsExactlyInAnyOrderElementsOf(expectedIds);
    }

}
