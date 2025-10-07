#@disabled
Feature: JVM memory utilities
  Memory information is retrieved and cached for performance optimization.

  Background:
    Given JVM total memory is set to 2147483648 bytes

  Scenario Outline: max memory is retrieved correctly
    Given JVM max memory is set to <maxMemoryBytes> bytes
    When max memory is requested
    Then the returned memory size should be <expectedBytes> bytes
    Examples:
      | maxMemoryBytes      | expectedBytes | comment                                             |
      | 1073741824          | 1073741824    |                                                     |
      | 9223372036854775807 | 2147483648    | max. long (i.e. no limit) is capped to total memory |
      | 536870912           | 536870912     |                                                     |

  Scenario: free memory is retrieved correctly
    Given JVM free memory is set to 268435456 bytes
    When free memory is requested
    Then the returned memory size should be 268435456 bytes

  Scenario: memory values are cached for performance
    Given JVM max memory is set to 1073741824 bytes
    When max memory is requested 3 times
    Then runtime should be accessed only once for max memory

  Scenario Outline: memory accessor handles different memory types
    Given JVM <memoryType> memory is set to <bytes> bytes
    When <memoryType> memory bytes are accessed directly
    Then the returned value should be <bytes> bytes
    Examples:
      | memoryType | bytes      |
      | total      | 2147483648 |
      | max        | 1073741824 |
      | free       | 536870912  |
