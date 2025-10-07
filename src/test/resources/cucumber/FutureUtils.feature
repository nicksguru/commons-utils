@utils #@disabled
Feature: Parallel execution of tasks using FutureUtils

  Scenario Outline: Supplying values in parallel
    Given <count> suppliers that return their index
    When the suppliers are executed in parallel with <threadLimit> threads
    Then <count> results should be collected in the original order
    And no exception should be thrown
    Examples:
      | count | threadLimit |
      | 5     | 2           |
      | 10    | 5           |
      | 20    | 10          |
      | 5     | 5           |

  Scenario Outline: Running tasks in parallel
    Given <count> runnables that store their execution order
    When the runnables are executed in parallel with <threadLimit> threads
    Then all <count> runnables should have been executed
    And no exception should be thrown
    Examples:
      | count | threadLimit |
      | 5     | 2           |
      | 10    | 5           |
      | 20    | 10          |
      | 5     | 5           |

  Scenario: Handling exceptions in parallel suppliers
    Given a supplier that throws an exception
    When the supplier is executed in parallel
    Then an exception should be thrown

  Scenario: Handling exceptions in parallel runnables
    Given a runnable that throws an exception
    When the runnable is executed in parallel
    Then an exception should be thrown

  Scenario: MDC context is preserved in parallel suppliers
    Given a supplier that checks MDC context
    And MDC context is set with key "testKey" and value "testValue"
    When the supplier is executed in parallel
    Then the supplier should have access to the MDC context
    And no exception should be thrown

  Scenario: MDC context is preserved in parallel runnables
    Given a runnable that checks MDC context
    And MDC context is set with key "testKey" and value "testValue"
    When the runnable is executed in parallel
    Then the runnable should have access to the MDC context
    And no exception should be thrown
