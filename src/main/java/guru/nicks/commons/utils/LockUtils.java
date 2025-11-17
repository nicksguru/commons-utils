package guru.nicks.commons.utils;

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
     * Executes the given resultSupplier using the optimistic read lock mode. If this mode fails (someone acquired a
     * write lock meanwhile, for example with {@link #returnWithExclusiveLock(StampedLock, Supplier)}), the
     * <b>resultSupplier is re-executed</b> with a real read lock, which means waiting until the exclusive lock gets
     * released.
     *
     * @param lock           lock to use: {@link StampedLock#tryOptimisticRead()} is called first and then
     *                       {@link StampedLock#readLock()} if needed
     * @param resultSupplier code to execute
     * @param <T>            return value type
     * @return result of the code execution
     */
    public static <T> T returnWithOptimisticReadOrRetry(StampedLock lock, Supplier<T> resultSupplier) {
        checkNotNull(lock, "lock");
        checkNotNull(resultSupplier, "resultSupplier");

        long stamp = lock.tryOptimisticRead();
        T result = resultSupplier.get();

        if (lock.validate(stamp)) {
            return result;
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
     * Same as {@link #returnWithOptimisticReadOrRetry(StampedLock, Supplier)}, just there's no value returned. The
     * method name is different because {@link Runnable} can be confused with {@link Supplier}.
     */
    public static void runWithOptimisticReadOrRetry(StampedLock lock, Runnable code) {
        returnWithOptimisticReadOrRetry(lock, TransformUtils.toSupplier(code));
    }

    /**
     * Executes the given code using an exclusive lock, waiting if necessary until the lock is released by its holder.
     *
     * @param lock           lock to use: {@link StampedLock#writeLock()} is called
     * @param resultSupplier code to execute
     * @param <T>            return value type
     * @return result of the code execution
     */
    public static <T> T returnWithExclusiveLock(StampedLock lock, Supplier<T> resultSupplier) {
        checkNotNull(lock, "lock");
        checkNotNull(resultSupplier, "resultSupplier");

        long stamp = lock.writeLock();

        try {
            return resultSupplier.get();
        } finally {
            lock.unlock(stamp);
        }
    }

    /**
     * Same as {@link #returnWithExclusiveLock(StampedLock, Supplier)}, just there's no value returned. The method name
     * is different because {@link Runnable} can be confused with {@link Supplier}.
     */
    public static void runWithExclusiveLock(StampedLock lock, Runnable code) {
        returnWithExclusiveLock(lock, TransformUtils.toSupplier(code));
    }

}
