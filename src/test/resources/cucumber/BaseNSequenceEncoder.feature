Feature: BaseNSequenceEncoder functionality
  Base10-to-BaseN numeric conversion using a precomputed char table
  So that sequences are encoded and decoded identically to the previous map/LinkedList implementation

  Scenario Outline: Encode/decode round-trip
    Given a BaseNSequenceEncoder with alphabet "<alphabet>"
    When sequence <sequence> is encoded
    Then the encoded value should be "<encoded>"
    And decoding "<encoded>" should return <sequence>

    Examples:
      | alphabet                                                              | sequence            | encoded                                                             |
      | 01                                                                    | 0                   | 0                                                                  |
      | 01                                                                    | 1                   | 1                                                                  |
      | 01                                                                    | 2                   | 10                                                                 |
      | 01                                                                    | 5                   | 101                                                                |
      | 01                                                                    | 255                 | 11111111                                                           |
      | 01                                                                    | 9223372036854775807 | 111111111111111111111111111111111111111111111111111111111111111   |
      | 0123456789                                                            | 0                   | 0                                                                  |
      | 0123456789                                                            | 255                 | 255                                                                |
      | 0123456789abcdef                                                      | 16                  | 10                                                                 |
      | 0123456789abcdef                                                      | 255                 | ff                                                                 |
      | 0123456789abcdef                                                      | 2748                | abc                                                                |
      | 0123456789abcdef                                                      | 4095                | fff                                                                |
      | 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz | 61                  | z                                                                  |
      | 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz | 62                  | 10                                                                 |
      | 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz | 3843                | zz                                                                 |
      | 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz | 3844                | 100                                                                |
      | 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz | 9223372036854775807 | AzL8n0Y58m7                                                        |

  Scenario: Max-length edge case encodes Long.MAX_VALUE in binary as 63 ones
    Given a BaseNSequenceEncoder with alphabet "01"
    When sequence 9223372036854775807 is encoded
    Then the encoded value should be "111111111111111111111111111111111111111111111111111111111111111"
    And the encoded value length should be 63

  Scenario: Leading zero characters are stripped when decoding
    Given a BaseNSequenceEncoder with alphabet "01"
    Then decoding "00000000000000000000000000000000000000000000000000000000001010" should return 10

  Scenario: Encoding a negative sequence fails
    Given a BaseNSequenceEncoder with alphabet "01"
    When sequence -1 is encoded
    Then an IllegalArgumentException naming "sequence" should be thrown

  Scenario: Encoding a null sequence fails
    Given a BaseNSequenceEncoder with alphabet "01"
    When a null sequence is encoded
    Then an IllegalArgumentException naming "sequence" should be thrown

  Scenario: Decoding a blank value fails
    Given a BaseNSequenceEncoder with alphabet "01"
    When a blank value is decoded
    Then an IllegalArgumentException naming "value" should be thrown

  Scenario: Decoding a value longer than the max encoded length fails
    Given a BaseNSequenceEncoder with alphabet "01"
    When a value of 64 characters is decoded
    Then an IllegalArgumentException naming "value" should be thrown

  Scenario: Decoding a value with an invalid character fails
    Given a BaseNSequenceEncoder with alphabet "01"
    When an invalid value "12" is decoded
    Then an IllegalArgumentException should be thrown

  Scenario: Constructing with a blank alphabet fails
    When a BaseNSequenceEncoder with alphabet " " is constructed
    Then an IllegalArgumentException naming "alphabet" should be thrown

  Scenario: Constructing with an alphabet containing whitespaces fails
    When a BaseNSequenceEncoder with alphabet "0 1" is constructed
    Then an IllegalArgumentException naming "alphabet" should be thrown

  Scenario: Constructing with an alphabet containing duplicates fails
    When a BaseNSequenceEncoder with alphabet "010" is constructed
    Then an IllegalArgumentException naming "alphabet" should be thrown
