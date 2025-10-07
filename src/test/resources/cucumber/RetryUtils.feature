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
