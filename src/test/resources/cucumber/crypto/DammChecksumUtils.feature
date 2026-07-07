@utils
Feature: DammChecksumUtils

  Scenario Outline: Compute checksum for decimal numbers
    Given a decimal payload "<Payload>"
    When Damm checksum is computed using DECIMAL implementation
    Then the checksum character should be "<Checksum>"
    Examples:
      | Payload | Checksum | Comment              |
      | 0       | 0        | single zero          |
      | 1       | 3        | single digit         |
      | 9       | 2        | highest single digit |
      | 12      | 5        | two digits           |
      | 123     | 4        | three digits         |
      | 1234    | 0        | four digits          |
      | 12345   | 9        | five digits          |
      | 99999   | 9        | all same digits      |
      | 54321   | 6        | descending digits    |

  Scenario Outline: Compute checksum for Crockford Base32 strings
    Given a Crockford Base32 payload "<Payload>"
    When Damm checksum is computed using CROCKFORD_BASE32 implementation
    Then the checksum character should be "<Checksum>"
    Examples:
      | Payload | Checksum | Comment                                                 |
      | 0       | 0        | checksum is the same as for '0' in decimal alphabet     |
      | 1       | 2        | checksum is not the same as for '1' in decimal alphabet |
      | A       | M        | single letter                                           |
      | Z       | X        | highest letter                                          |
      | 12      | 0        | two digits                                              |
      | ABC     | 1        | three letters                                           |
      | 123AB   | 5        | mixed alphanumeric                                      |
      | ZZZZZ   | P        | all same letters                                        |

  Scenario Outline: Validate valid checksums for decimal numbers
    Given a decimal value with checksum "<Value>"
    When the value is validated using DECIMAL implementation
    Then the value should be valid
    Examples:
      | Value  | Comment                 |
      | 00     | zero with checksum      |
      | 13     | single digit + checksum |
      | 1234   | valid checksum          |
      | 123459 | valid checksum          |

  Scenario Outline: Validate valid checksums for Crockford Base32 strings
    Given a Crockford Base32 value with checksum "<Value>"
    When the value is validated using CROCKFORD_BASE32 implementation
    Then the value should be valid
    Examples:
      | Value  | Comment                 |
      | 00     | zero with checksum      |
      | 120    | single digit + checksum |
      | ABC1   | valid checksum          |
      | 123AB5 | valid checksum          |

  Scenario Outline: Detect invalid checksums for decimal numbers
    Given a decimal value with invalid checksum "<Value>"
    When the value is validated using DECIMAL implementation
    Then the value should be invalid
    Examples:
      | Value  | Comment        |
      | 01     | wrong checksum |
      | 11     | wrong checksum |
      | 1230   | wrong checksum |
      | 123450 | wrong checksum |

  Scenario Outline: Detect invalid checksums for Crockford Base32 strings
    Given a Crockford Base32 value with invalid checksum "<Value>"
    When the value is validated using CROCKFORD_BASE32 implementation
    Then the value should be invalid
    Examples:
      | Value   | Comment        |
      | 01      | wrong checksum |
      | 1A      | wrong checksum |
      | ABC0    | wrong checksum |
      | 123ABC0 | wrong checksum |

  Scenario Outline: Detect single-digit errors in decimal numbers
    Given a valid decimal checksummed value "<Original>"
    When a single digit is corrupted to "<Corrupted>"
    And the value is validated using DECIMAL implementation
    Then the value should be invalid
    Examples:
      | Original | Corrupted | Comment              |
      | 12340    | 02340     | first digit changed  |
      | 12340    | 19340     | second digit changed |
      | 12340    | 12940     | third digit changed  |
      | 12340    | 12390     | fourth digit changed |
      | 123456   | 023456    | first digit changed  |
      | 123456   | 123956    | fourth digit changed |

  Scenario Outline: Detect single-digit errors in Crockford Base32 strings
    Given a valid Crockford Base32 checksummed value "<Original>"
    When a single character is corrupted to "<Corrupted>"
    And the value is validated using CROCKFORD_BASE32 implementation
    Then the value should be invalid
    Examples:
      | Original | Corrupted | Comment             |
      | ABC5     | 0BC5      | first char changed  |
      | ABC5     | ACC5      | second char changed |
      | ABC5     | AB05      | third char changed  |
      | ABC5     | ABCA      | fourth char changed |
      | 123AB5   | 023AB5    | first char changed  |
      | 123AB5   | 123A05    | fourth char changed |

  Scenario Outline: Detect adjacent transposition errors in decimal numbers
    Given a valid decimal checksummed value "<Original>"
    When adjacent digits are transposed to "<Transposed>"
    And the value is validated using DECIMAL implementation
    Then the value should be invalid
    Examples:
      | Original | Transposed | Comment             |
      | 12340    | 21340      | first two swapped   |
      | 12340    | 13240      | middle two swapped  |
      | 12340    | 12430      | last two swapped    |
      | 123456   | 213456     | first two swapped   |
      | 123456   | 132456     | second pair swapped |
      | 123456   | 124356     | third pair swapped  |

  Scenario Outline: Detect adjacent transposition errors in Crockford Base32 strings
    Given a valid Crockford Base32 checksummed value "<Original>"
    When adjacent characters are transposed to "<Transposed>"
    And the value is validated using CROCKFORD_BASE32 implementation
    Then the value should be invalid
    Examples:
      | Original | Transposed | Comment             |
      | ABC5     | BAC5       | first two swapped   |
      | ABC5     | ACB5       | middle two swapped  |
      | ABC5     | AB5C       | last two swapped    |
      | 123AB5   | 213AB5     | first two swapped   |
      | 123AB5   | 132AB5     | second pair swapped |
      | 123AB5   | 12A3B5     | third pair swapped  |

  Scenario: Validate empty string using DECIMAL
    Given an empty string
    When the value is validated using DECIMAL implementation
    Then the value should be invalid

  Scenario: Validate empty string using CROCKFORD_BASE32
    Given an empty string
    When the value is validated using CROCKFORD_BASE32 implementation
    Then the value should be invalid

  Scenario: Validate single character using DECIMAL
    Given a single character "5"
    When the value is validated using DECIMAL implementation
    Then the value should be invalid

  Scenario: Validate single character using CROCKFORD_BASE32
    Given a single character "A"
    When the value is validated using CROCKFORD_BASE32 implementation
    Then the value should be invalid

  Scenario: Handle invalid character in decimal payload
    Given a decimal payload with invalid character "12A34"
    When Damm checksum is computed using DECIMAL implementation
    Then an exception should be thrown

  Scenario: Handle invalid character in Crockford Base32 payload
    Given a Crockford Base32 payload with invalid character "12I34"
    When Damm checksum is computed using CROCKFORD_BASE32 implementation
    Then an exception should be thrown

  Scenario: Validate value with invalid character using DECIMAL
    Given a decimal value with invalid character "12A34"
    When the value is validated using DECIMAL implementation
    Then the value should be invalid

  Scenario: Validate value with invalid character using CROCKFORD_BASE32
    Given a Crockford Base32 value with invalid character "12I34"
    When the value is validated using CROCKFORD_BASE32 implementation
    Then the value should be invalid

  Scenario: Compute checksum for empty decimal payload
    Given an empty decimal payload
    When Damm checksum is computed using DECIMAL implementation
    Then the checksum character should be "0"

  Scenario: Compute checksum for empty Crockford Base32 payload
    Given an empty Crockford Base32 payload
    When Damm checksum is computed using CROCKFORD_BASE32 implementation
    Then the checksum character should be "0"

  Scenario Outline: Verify checksum is deterministic for decimal
    Given a decimal payload "<Payload>"
    When Damm checksum is computed twice using DECIMAL implementation
    Then both checksums should be identical
    Examples:
      | Payload |
      | 0       |
      | 123     |
      | 99999   |

  Scenario Outline: Verify checksum is deterministic for Crockford Base32
    Given a Crockford Base32 payload "<Payload>"
    When Damm checksum is computed twice using CROCKFORD_BASE32 implementation
    Then both checksums should be identical
    Examples:
      | Payload |
      | 0       |
      | ABC     |
      | ZZZZZ   |
