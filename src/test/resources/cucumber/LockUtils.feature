@utils #@disabled
Feature: LockUtils functionality
  The LockUtils class should provide thread-safe operations
  So that concurrent access to shared resources is properly managed

  Scenario: Optimistic read lock with no contention
    Given a StampedLock instance
    When the optimistic read lock is used with no write contention
    Then the operation should complete successfully
    And the optimistic read should not be retried

  Scenario: Optimistic read lock with write contention
    Given a StampedLock instance
    And a background thread holding a write lock
    When the optimistic read lock is used with write contention
    Then the operation should complete successfully
    And the optimistic read should be retried with a real read lock

  Scenario: Exclusive write lock
    Given a StampedLock instance
    When the exclusive write lock is used
    Then the operation should complete successfully
    And the write lock should be released after completion

  Scenario: Multiple threads using optimistic read
    Given a StampedLock instance
    When multiple threads use optimistic read concurrently
    Then all operations should complete successfully
    And no thread synchronization issues should occur

  Scenario: Multiple threads using write lock
    Given a StampedLock instance
    When multiple threads compete for the write lock
    Then all operations should complete successfully
    And each thread should get exclusive access

  Scenario: Null lock parameter handling
    Given a null StampedLock instance
    When the optimistic read lock is used
    Then the exception message should contain "lock"

  Scenario: Null code parameter handling
    Given a StampedLock instance
    When the optimistic read lock is used with null code
    Then the exception message should contain "resultSupplier"
