@utils #@disabled
Feature: Hash Utilities
  Various hash algorithms are provided for different use cases

  Scenario: Computing XXHASH3 with default length
    Given input string "test data"
    And hash algorithm "XXHASH3"
    When the hash is computed
    Then the hash result should have length 8
    And no exception should be thrown

  Scenario: Computing XXHASH3 with (the only) valid length
    Given input string "test data"
    And hash algorithm "XXHASH3"
    And hash length 8
    When the hash is computed with specified length
    Then the hash result should have length 8
    And no exception should be thrown

  Scenario: Computing XXHASH3 with invalid length
    Given input string "test data"
    And hash algorithm "XXHASH3"
    And hash length 16
    When the hash is computed with specified length
    Then the exception message should contain "must be between 8 and 8"

  Scenario: Computing SHA3_512 with default length
    Given input string "test data"
    And hash algorithm "SHA3_512"
    When the hash is computed
    Then the hash result should have length 64
    And no exception should be thrown

  Scenario: Computing SHA3_512 with custom length
    Given input string "test data"
    And hash algorithm "SHA3_512"
    And hash length 32
    When the hash is computed with specified length
    Then the hash result should have length 32
    And no exception should be thrown

  Scenario: Computing SHA3_512 with invalid length
    Given input string "test data"
    And hash algorithm "SHA3_512"
    And hash length 128
    When the hash is computed with specified length
    Then the exception message should contain "hash length"

  Scenario: Computing LUHN_DIGIT for valid input
    Given input string "7992739871"
    And hash algorithm "LUHN_DIGIT"
    When the hash is computed
    Then the hash result as string should be "3"
    And no exception should be thrown

  Scenario: Computing LUHN_DIGIT for invalid input
    Given input string "000000"
    And hash algorithm "LUHN_DIGIT"
    When the hash is computed
    Then the exception message should contain "zero"

  Scenario: Computing ISIN_DIGIT for invalid input
    Given input string "000000"
    And hash algorithm "ISIN_DIGIT"
    When the hash is computed
    Then the exception message should contain "zero"

  Scenario: Computing VERHOEFF for valid input
    Given input string "236"
    And hash algorithm "VERHOEFF"
    When the hash is computed
    Then the hash result as string should be "3"
    And no exception should be thrown

  Scenario: Computing VERHOEFF for all-zero input
    Given input string "000000"
    And hash algorithm "VERHOEFF"
    When the hash is computed
    Then no exception should be thrown

  Scenario: Verify max hash lengths
    Then the max hash length for "XXHASH3" should be 8
    And the max hash length for "SHA3_512" should be 64
    And the max hash length for "LUHN_DIGIT" should be 1
    And the max hash length for "VERHOEFF" should be 1

  Scenario: Verify default hash lengths
    Then the default hash length for "XXHASH3" should be 8
    And the default hash length for "SHA3_512" should be 64
    And the default hash length for "LUHN_DIGIT" should be 1
    And the default hash length for "VERHOEFF" should be 1

  Scenario: Batch testing of hash operations
    When the following hash operations are performed:
      | input       | algorithm  | length | expectedOutput                   | expectException | comments                     |
      | 000000      | LUHN_DIGIT |        |                                  | true            | all-zero                     |
      | 12345       | LUHN_DIGIT |        | 5                                | false           |                              |
      | 000000      | ISIN_DIGIT |        |                                  | true            | all-zero                     |
      | 12345       | ISIN_DIGIT |        | 5                                | false           |                              |
      | ABC123      | ISIN_DIGIT |        | 1                                | false           |                              |
      | abc123      | ISIN_DIGIT |        | 1                                | false           | treat lowercase as uppercase |
      | +-          | ISIN_DIGIT |        |                                  | true            | non-alphanumeric             |
      | 142857      | VERHOEFF   |        | 0                                | false           |                              |
      | 000000      | VERHOEFF   |        | 6                                | false           | all-zero                     |
      | ABC123      | VERHOEFF   |        |                                  | true            | non-numeric                  |
      | test string | XXHASH3    | 8      | 3D5061310B23B3B9                 | false           |                              |
      | test string | XXHASH3    | 9      |                                  | true            |                              |
      | hello world | SHA3_512   | 16     | 840006653E9AC9E95117A15C915CAAB8 | false           |                              |
      | hello world | SHA3_512   | 1000   |                                  | true            |                              |
