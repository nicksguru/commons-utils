package guru.nicks.commons.utils;

import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;

import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Lock-related utility methods.
 */
@UtilityClass
public class LockUtils {

    /**
     * Executes the given supplier using the optimistic read lock mode. If someone acquired a write lock meanwhile, for
     * example with {@link #withExclusiveLock(StampedLock, Supplier)}, the
     * <b>supplier may be re-executed</b> with a read lock, which means waiting until the exclusive lock gets
     * released.
     *
     * @param lock           lock to use: {@link StampedLock#tryOptimisticRead()} is called first and then
     *                       {@link StampedLock#readLock()} if needed
     * @param resultSupplier code to execute (see also {@link TransformUtils#toSupplier(Runnable)})
     * @param <T>            return value type
     * @return result of the code execution
     */
    @Nullable
    public static <T> T withOptimisticReadOrRetry(StampedLock lock, Supplier<T> resultSupplier) {
        checkNotNull(lock, "lock");
        checkNotNull(resultSupplier, "resultSupplier");

        long stamp = lock.tryOptimisticRead();

        // stamp 0 means a write lock is currently held: the optimistic read can never validate, so skip the wasted
        // supplier computation and fall through to the pessimistic path below
        if (stamp != 0) {
            T result = resultSupplier.get();

            if (lock.validate(stamp)) {
                return result;
            }
        }

        // someone acquired write (i.e. exclusive) lock meanwhile - retry with a real lock
        stamp = lock.readLock();
        try {
            return resultSupplier.get();
        } finally {
            lock.unlock(stamp);
        }
    }

    /**
     * Executes the given code using an exclusive lock, waiting if necessary until the lock is released by its holder.
     *
     * @param lock           lock to use: {@link StampedLock#writeLock()} is called
     * @param resultSupplier code to execute (see also {@link TransformUtils#toSupplier(Runnable)})
     * @param <T>            return value type
     * @return result of the code execution
     */
    @Nullable
    public static <T> T withExclusiveLock(StampedLock lock, Supplier<T> resultSupplier) {
        checkNotNull(lock, "lock");
        checkNotNull(resultSupplier, "resultSupplier");

        long stamp = lock.writeLock();

        try {
            return resultSupplier.get();
        } finally {
            lock.unlock(stamp);
        }
    }

}
