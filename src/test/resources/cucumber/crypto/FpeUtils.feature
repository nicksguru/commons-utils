#@disabled
Feature: FPE Utils

  Scenario Outline: Creating an FF1 sequence encryptor with invalid arguments
    When an FF1 sequence encryptor is created with key "<key>", tweak "<tweak>", alphabet "0123456789", and padding to <zeroPadPositions> positions
    Then an exception should be thrown
    Examples:
      | zeroPadPositions | key              | tweak        |
      | -5               | secretkey16bytes | secret-tweak |
      | 8                |                  | secret-tweak |
      | 8                | secretkey16bytes |              |

  Scenario Outline: Encrypting and decrypting sequence values
    Given an FF1 sequence encryptor is created with key "secretkey16bytes", tweak "secret-tweak", alphabet "0123456789", and padding to 8 positions
    And no exception should be thrown
    And the sequence value supplier will return "<sequenceValue>"
    When the next encrypted value is requested
    Then no exception should be thrown
    And the encrypted value is not blank
    And the encrypted value should be "<encryptedValue>"
    And decrypting the value returns "<decryptedValue>"
    And no exception should be thrown
    Examples:
      | sequenceValue | encryptedValue | decryptedValue |
      | 1             | 49072284       | 00000001       |
      | 12345         | 90000597       | 00012345       |
      | 123456789     | 725780945      | 123456789      |

  Scenario Outline: Getting the next encrypted value with an invalid sequence value
    Given an FF1 sequence encryptor is created with key "secretkey16bytes", tweak "secret-tweak", alphabet "0123456789", and padding to 8 positions
    And no exception should be thrown
    And the sequence value supplier will return "<sequenceValue>"
    When the next encrypted value is requested
    Then the exception message should contain "must not be blank"
    Examples:
      | sequenceValue |
      |               |

  Scenario: Decrypting an invalid value
    Given an FF1 sequence encryptor is created with key "secretkey16bytes", tweak "secret-tweak", alphabet "0123456789", and padding to 8 positions
    And no exception should be thrown
    When the value "not-a-number" is decrypted
    Then the exception message should contain "Input character is missing from the alphabet"

  Scenario: Unsupported key size
    Given an FF1 sequence encryptor is created with key "key10bytes", tweak "secret-tweak", alphabet "0123456789", and padding to 8 positions
    Then the exception message should contain "key size must be "

  Scenario Outline: Encounter all-zeroes encryption result
    Given an FF1 sequence encryptor is created with key "secretkey16bytes", tweak "secret-tweak", alphabet "0123456789", and padding to 6 positions
    And no exception should be thrown
    And the sequence value supplier will return "<sequenceNumber>"
    When the next encrypted value is requested
    And the exception message should contain "Sequence value must be different from the previous one"
    Examples:
      | sequenceNumber | comments                   |
      | 161626         | gets encrypted to '000000' |

  Scenario: Work around all-zeroes case
    Given an FF1 sequence encryptor is created with key "secretkey16bytes", tweak "secret-tweak", alphabet "0123456789", and padding to 6 positions
    And no exception should be thrown
    # the first value gets encrypted to '000000', therefore the next value should be obtained
    And the sequence number supplier will return "161626" and then "161627"
    When the next encrypted value is requested
    Then no exception should be thrown
    Then the encrypted value should be "117653"

  Scenario: Too short custom alphabet
    Given an FF1 sequence encryptor is created with key "secretkey16bytes", tweak "secret-tweak", alphabet "0abcde", and padding to 6 positions
    Then the exception message should contain "alphabet length must be between 10 and 256 (inclusive)"

  Scenario Outline: Encrypting and decrypting non-decimal values
    Given an FF1 sequence encryptor is created with key "secretkey16bytes", tweak "secret-tweak", alphabet "01234abcdey", and padding to 6 positions
    And no exception should be thrown
    And the sequence value supplier will return "<sequenceValue>"
    When the next encrypted value is requested
    Then no exception should be thrown
    And the encrypted value is not blank
    And the encrypted value should be "<encryptedValue>"
    And decrypting the value returns "<decryptedValue>"
    And no exception should be thrown
    Examples:
      | sequenceValue | encryptedValue | decryptedValue |
      | a             | 40d02b         | 00000a         |
      | ab            | 0b04ay         | 0000ab         |
      | abc           | 43dc1a         | 000abc         |
