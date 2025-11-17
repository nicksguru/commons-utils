package guru.nicks.commons.service;

import java.time.Duration;
import java.util.function.Supplier;

public interface DistributedLockService {

    /**
     * Executes the given code using an exclusive distributed lock, waiting if necessary until the lock is released by
     * its holder. The lock is supposed to be stored in a dedicated persistent storage, such as Redis, thus making it
     * suitable for microservice-based environments.
     * <p>
     * For example, if there are multiple instances of the same microservice, in-memory locks are seen by one of the
     * instances only, which may or may not be the expected behavior.
     * <p>
     * After the code has been executed (with or without an exception), the lock is released.
     *
     * @param lockName lock name
     * @param lockTtl  time to live (the smallest recognizable time unit is milliseconds) for the <b>new</b> lock
     * @param code     code to execute
     * @param <T>      return value type
     */
    <T> T withExclusiveLock(String lockName, Duration lockTtl, Supplier<T> code);

}
