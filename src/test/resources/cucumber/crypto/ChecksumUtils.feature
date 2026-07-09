@utils
Feature: ChecksumUtils

  Scenario Outline: Compute checksum for scalars (and treating nulls as empty strings)
    Given input is "<Input>"
    When JSON checksum is computed for scalar input
    Then output should be "<Checksum>"
    Examples:
      | Input           | Checksum                                     | Comment                                |
      | null            | 47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU= | 'null' means null in this test         |
      |                 | 47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU= | empty input string                     |
      | test            | n4bQgYhMfWWaL+qgxVrQFaO/TxsrC4Is0V1sFbDwCgg= |                                        |
      | 12345           | WZRHGrsBESr8wYFZ9sx0tPURuZgG2lmzyvWpwXPKz8U= | a numeric string (not a binary number) |
      | [one1)!/!(two2] | R0xmJjnQ5ZqsXAJXCXiXRctdMqHJ/N7CqVjJxBeCQd4= |                                        |

  Scenario Outline: Compute checksum for objects
    Given test user has name "<Name>" and email "<Email>"
    When JSON checksum is computed for test user
    Then output should be "<Checksum>"
    And sorted JSON should be "<Sorted JSON>"
    Examples:
      | Name  | Email             | Checksum                                     | Sorted JSON                                                  |
      | test1 | test1@example.com | A7F47u2e+3Ap5ezlGE569UMpxCk55NPJks3ZriSjH6c= | {\\"email\\":\\"test1@example.com\\",\\"name\\":\\"test1\\"} |
      | test2 | test2@example.com | p3GxJd+45GVcEaOwUSq9iSR8YgMPzQheD4OKsXR8Ip8= | {\\"email\\":\\"test2@example.com\\",\\"name\\":\\"test2\\"} |

  Scenario Outline: Compute checksum for complex objects
    Given test user has name "<Name>" and email "<Email>"
    When JSON checksum is computed for test user
    Then output should be "<Checksum>"
    And sorted JSON should be "<Sorted JSON>"
    Examples:
      | Name       | Email               | Checksum                                     | Sorted JSON                                                         |
      | John Doe   | john@example.com    | 4AKxzkPmP4Hm4Yql5M7ROFPu7PtoxtGjMUXl5RrB4bc= | {\\"email\\":\\"john@example.com\\",\\"name\\":\\"John Doe\\"}      |
      | Jane Smith | jane.smith@test.com | P8kzm7ILGjlAHR7qPfVArt3s5CeVGp4jyBwLHzI0Lb8= | {\\"email\\":\\"jane.smith@test.com\\",\\"name\\":\\"Jane Smith\\"} |

  Scenario Outline: Verify checksum determinism for same input
    Given input is "<Input>"
    When JSON checksum is computed for scalar input
    Then output should be "<Checksum>"
    And JSON checksum is computed again for scalar input
    Then both checksums should be identical
    Examples:
      | Input | Checksum                                     |
      | test  | n4bQgYhMfWWaL+qgxVrQFaO/TxsrC4Is0V1sFbDwCgg= |
      | 12345 | WZRHGrsBESr8wYFZ9sx0tPURuZgG2lmzyvWpwXPKz8U= |

  Scenario Outline: Verify checksum differs for different inputs
    Given input is "<FirstInput>"
    When JSON checksum is computed for scalar input
    Then output should be "<Checksum>"
    Given input is "<SecondInput>"
    When JSON checksum is computed again for scalar input
    Then checksums should be different
    Examples:
      | FirstInput | SecondInput | Checksum                                     |
      | test       | Test        | n4bQgYhMfWWaL+qgxVrQFaO/TxsrC4Is0V1sFbDwCgg= |
      | 12345      | 12354       | WZRHGrsBESr8wYFZ9sx0tPURuZgG2lmzyvWpwXPKz8U= |

  Scenario: Verify checksum format
    Given input is "test"
    When JSON checksum is computed for scalar input
    Then output should match Base64 format
    And output length should be 44
