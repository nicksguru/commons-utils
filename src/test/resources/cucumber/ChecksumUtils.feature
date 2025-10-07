@utils #@disabled
Feature: ChecksumUtils

  Scenario Outline: Compute checksum for scalars (and treating nulls as empty strings)
    Given input is "<Input>"
    When checksum is computed for scalar input
    Then output should be "<Checksum>"
    Examples:
      | Input           | Checksum                                     | Comments                               |
      | null            | 47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU= | 'null' means null in this test         |
      |                 | 47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU= | empty input string                     |
      | test            | n4bQgYhMfWWaL+qgxVrQFaO/TxsrC4Is0V1sFbDwCgg= |                                        |
      | 12345           | WZRHGrsBESr8wYFZ9sx0tPURuZgG2lmzyvWpwXPKz8U= | a numeric string (not a binary number) |
      | [one1)!/!(two2] | R0xmJjnQ5ZqsXAJXCXiXRctdMqHJ/N7CqVjJxBeCQd4= |                                        |

  Scenario Outline: Compute checksum for objects
    Given test user has name "<Name>" and email "<Email>"
    When checksum is computed for test user
    Then output should be "<Checksum>"
    And sorted JSON should be "<Sorted JSON>"
    Examples:
      | Name  | Email             | Checksum                                     | Sorted JSON                                                  |
      | test1 | test1@example.com | A7F47u2e+3Ap5ezlGE569UMpxCk55NPJks3ZriSjH6c= | {\\"email\\":\\"test1@example.com\\",\\"name\\":\\"test1\\"} |
      | test2 | test2@example.com | p3GxJd+45GVcEaOwUSq9iSR8YgMPzQheD4OKsXR8Ip8= | {\\"email\\":\\"test2@example.com\\",\\"name\\":\\"test2\\"} |
