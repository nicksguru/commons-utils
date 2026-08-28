@utils #@disabled
Feature: RetryUtils
  Retry operations with exponential backoff
  So that transient failures are handled gracefully

  Scenario: Successful execution on first attempt with retries disabled
    Given base delay is 1 ms
    And maximum 0 retry attempts
    When execute operation that succeeds immediately
    Then operation should complete successfully
    # no retries
    And total number of code invocations should be 1

  Scenario: Successful execution on first attempt with retries enabled
    Given base delay is 1 ms
    And maximum 2 retry attempts
    When execute operation that succeeds immediately
    Then operation should complete successfully
    # no retries
    And total number of code invocations should be 1

  Scenario: Failure with retries disabled
    Given base delay is 1 ms
    And maximum 0 retry attempts
    When execute operation that always fails
    Then operation should fail with exception
    And total number of code invocations should be 1

  Scenario: Successful retry after multiple attempts
    Given base delay is 1 ms
    And maximum 4 retry attempts
    # initial attempt + 1 retry
    When execute operation that succeeds after 2 invocations
    Then operation should complete successfully
    And total number of code invocations should be 2

  Scenario: Failure after exceeding max attempts
    Given base delay is 1 ms
    And maximum 2 retry attempts
    When execute operation that always fails
    Then operation should fail with exception
    # initial attempt + 2 retries
    And total number of code invocations should be 3

  Scenario: Exception logger runs after every failed attempt including the final one
    Given base delay is 1 ms
    And maximum 2 retry attempts
    When execute operation that always fails
    Then operation should fail with exception
    # initial attempt + 2 retries, each one logged
    And total number of code invocations should be 3
    And the exception logger should have been invoked 3 times
    And the last logger invocation should report 3 retries made

  Scenario: Interrupted retry aborts instead of busy-retrying
    Given base delay is 1 ms
    And maximum 5 retry attempts
    When execute operation that fails and interrupts the thread on the second invocation
    Then the retry should be aborted by interruption
    # the interrupt arrives during the sleep after the second attempt
    And total number of code invocations should be 2
    And the interrupt flag should be set
