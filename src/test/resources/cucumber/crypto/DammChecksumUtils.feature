@utils
Feature: DammChecksumUtils

  Scenario Outline: Compute checksum for decimal numbers
    Given a payload "<Payload>"
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
    Given a payload "<Payload>"
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
    Given a value with checksum "<Value>"
    When the value is validated using DECIMAL implementation
    Then the value should be valid
    Examples:
      | Value  | Comment                 |
      | 00     | zero with checksum      |
      | 13     | single digit + checksum |
      | 1234   | valid checksum          |
      | 123459 | valid checksum          |

  Scenario Outline: Validate valid checksums for Crockford Base32 strings
    Given a value with checksum "<Value>"
    When the value is validated using CROCKFORD_BASE32 implementation
    Then the value should be valid
    Examples:
      | Value  | Comment                 |
      | 00     | zero with checksum      |
      | 120    | single digit + checksum |
      | ABC1   | valid checksum          |
      | 123AB5 | valid checksum          |

  Scenario Outline: Detect invalid checksums for decimal numbers
    Given a value with invalid checksum "<Value>"
    When the value is validated using DECIMAL implementation
    Then the value should be invalid
    Examples:
      | Value  | Comment        |
      | 01     | wrong checksum |
      | 11     | wrong checksum |
      | 1230   | wrong checksum |
      | 123450 | wrong checksum |

  Scenario Outline: Detect invalid checksums for Crockford Base32 strings
    Given a value with invalid checksum "<Value>"
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
    Given a payload with invalid character "12A34"
    When Damm checksum is computed using DECIMAL implementation
    Then an exception should be thrown

  Scenario: Handle invalid character in Crockford Base32 payload
    Given a payload with invalid character "12I34"
    When Damm checksum is computed using CROCKFORD_BASE32 implementation
    Then an exception should be thrown

  Scenario: Validate value with invalid character using DECIMAL
    Given a value with invalid character "12A34"
    When the value is validated using DECIMAL implementation
    Then the value should be invalid

  Scenario: Validate value with invalid character using CROCKFORD_BASE32
    Given a value with invalid character "12I34"
    When the value is validated using CROCKFORD_BASE32 implementation
    Then the value should be invalid

  Scenario: Compute checksum for empty decimal payload
    Given an empty payload
    When Damm checksum is computed using DECIMAL implementation
    Then the checksum character should be "0"

  Scenario: Compute checksum for empty Crockford Base32 payload
    Given an empty payload
    When Damm checksum is computed using CROCKFORD_BASE32 implementation
    Then the checksum character should be "0"

  Scenario Outline: Verify checksum is deterministic for decimal
    Given a payload "<Payload>"
    When Damm checksum is computed twice using DECIMAL implementation
    Then both Damm checksums should be identical
    Examples:
      | Payload |
      | 0       |
      | 123     |
      | 99999   |

  Scenario Outline: Verify checksum is deterministic for Crockford Base32
    Given a payload "<Payload>"
    When Damm checksum is computed twice using CROCKFORD_BASE32 implementation
    Then both Damm checksums should be identical
    Examples:
      | Payload |
      | 0       |
      | ABC     |
      | ZZZZZ   |

  Scenario Outline: Compute checksum for alphanumeric strings
    Given a payload "<Payload>"
    When Damm checksum is computed using ALPHANUMERIC implementation
    Then the checksum character should be "<Checksum>"
    Examples:
      | Payload | Checksum | Comment                  |
      | 0       | 0        | single zero              |
      | 1       | 2        | single digit             |
      | 9       | A        | highest single digit     |
      | A       | B        | single uppercase letter  |
      | Z       | a        | highest uppercase letter |
      | a       | b        | single lowercase letter  |
      | z       | 1        | highest lowercase letter |
      | 12      | 0        | two digits               |
      | AB      | 0        | two uppercase letters    |
      | ab      | 0        | two lowercase letters    |
      | 123     | 4        | three digits             |
      | ABC     | D        | three uppercase letters  |
      | abc     | d        | three lowercase letters  |
      | 1234    | 0        | four digits              |
      | ABCD    | 0        | four uppercase letters   |
      | abcd    | 0        | four lowercase letters   |
      | 12345   | 6        | five digits              |
      | ABCDE   | F        | five uppercase letters   |
      | abcde   | f        | five lowercase letters   |
      | 99999   | M        | all same digits          |
      | ZZZZZ   | m        | all same uppercase       |
      | zzzzz   | D        | all same lowercase       |
      | 54321   | Q        | descending digits        |
      | ZYXWV   | u        | descending uppercase     |
      | zyxwv   | L        | descending lowercase     |
      | 123AB   | u        | mixed alphanumeric       |
      | 123ab   | n        | mixed alphanumeric       |
      | AB12cd  | 0        | mixed alphanumeric       |

  Scenario Outline: Validate valid checksums for alphanumeric strings
    Given a value with checksum "<Value>"
    When the value is validated using ALPHANUMERIC implementation
    Then the value should be valid
    Examples:
      | Value  | Comment                 |
      | 00     | zero with checksum      |
      | 12     | single digit + checksum |
      | 1234   | valid checksum          |
      | 123456 | valid checksum          |
      | AB0    | valid checksum          |
      | ABCD   | valid checksum          |
      | ABCDEF | valid checksum          |
      | ab0    | valid checksum          |
      | abcd   | valid checksum          |
      | abcdef | valid checksum          |
      | 123ABu | valid checksum          |
      | 123abn | valid checksum          |

  Scenario Outline: Detect invalid checksums for alphanumeric strings
    Given a value with invalid checksum "<Value>"
    When the value is validated using ALPHANUMERIC implementation
    Then the value should be invalid
    Examples:
      | Value   | Comment        |
      | 01      | wrong checksum |
      | 11      | wrong checksum |
      | 1230    | wrong checksum |
      | 123450  | wrong checksum |
      | ABC0    | wrong checksum |
      | ABCDEA  | wrong checksum |
      | 123ABC0 | wrong checksum |

  Scenario Outline: Detect single-character errors in alphanumeric strings
    Given a valid alphanumeric checksummed value "<Original>"
    When a single character is corrupted to "<Corrupted>"
    And the value is validated using ALPHANUMERIC implementation
    Then the value should be invalid
    Examples:
      | Original | Corrupted | Comment             |
      | ABCD     | 0BCD      | first char changed  |
      | ABCD     | A0CD      | second char changed |
      | ABCD     | AB0D      | third char changed  |
      | ABCD     | ABC0      | fourth char changed |
      | 123ABu   | 023ABu    | first char changed  |
      | 123ABu   | 123A0u    | fifth char changed  |
      | abcd     | 0bcd      | first char changed  |
      | abcd     | a0cd      | second char changed |
      | abcd     | ab0d      | third char changed  |
      | abcd     | abc0      | fourth char changed |

  Scenario Outline: Detect adjacent transposition errors in alphanumeric strings
    Given a valid alphanumeric checksummed value "<Original>"
    When adjacent characters are transposed to "<Transposed>"
    And the value is validated using ALPHANUMERIC implementation
    Then the value should be invalid
    Examples:
      | Original | Transposed | Comment             |
      | ABCD     | BACD       | first two swapped   |
      | ABCD     | ACBD       | middle two swapped  |
      | ABCD     | ABDC       | last two swapped    |
      | 123ABu   | 213ABu     | first two swapped   |
      | 123ABu   | 132ABu     | second pair swapped |
      | 123ABu   | 12A3Bu     | third pair swapped  |
      | abcd     | bacd       | first two swapped   |
      | abcd     | acbd       | middle two swapped  |
      | abcd     | abdc       | last two swapped    |

  Scenario: Validate empty string using ALPHANUMERIC
    Given an empty string
    When the value is validated using ALPHANUMERIC implementation
    Then the value should be invalid

  Scenario: Validate single character using ALPHANUMERIC
    Given a single character "A"
    When the value is validated using ALPHANUMERIC implementation
    Then the value should be invalid

  Scenario: Handle invalid character in alphanumeric payload
    Given a payload with invalid character "12@34"
    When Damm checksum is computed using ALPHANUMERIC implementation
    Then an exception should be thrown

  Scenario: Validate value with invalid character using ALPHANUMERIC
    Given a value with invalid character "12@34"
    When the value is validated using ALPHANUMERIC implementation
    Then the value should be invalid

  Scenario: Compute checksum for empty alphanumeric payload
    Given an empty payload
    When Damm checksum is computed using ALPHANUMERIC implementation
    Then the checksum character should be "0"

  Scenario Outline: Verify checksum is deterministic for alphanumeric
    Given a payload "<Payload>"
    When Damm checksum is computed twice using ALPHANUMERIC implementation
    Then both Damm checksums should be identical
    Examples:
      | Payload |
      | 0       |
      | ABC     |
      | abc     |
      | ZZZZZ   |
      | zzzzz   |
      | 123AB   |
      | 123ab   |
