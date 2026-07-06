#@disabled
Feature: FPE Utils

  Scenario Outline: Creating an FF31 sequence encryptor with invalid arguments
    When an FF31 sequence encryptor is created with key "<key>", tweak "<tweak>", alphabet "0123456789", and padding to <zeroPadPositions> positions
    Then an exception should be thrown
    Examples:
      | zeroPadPositions | key              | tweak   |
      | -5               | secretkey16bytes | 7bytes! |
      | 8                |                  | 7bytes! |
      | 8                | secretkey16bytes |         |

  Scenario Outline: Encrypting and decrypting sequence values
    Given an FF31 sequence encryptor is created with key "secretkey16bytes", tweak "7bytes!", alphabet "0123456789", and padding to 8 positions
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
      | 1             | 25113153       | 00000001       |
      | 12345         | 58114689       | 00012345       |
      | 123456789     | 559969780      | 123456789      |

  Scenario Outline: Getting the next encrypted value with an invalid sequence value
    Given an FF31 sequence encryptor is created with key "secretkey16bytes", tweak "7bytes!", alphabet "0123456789", and padding to 8 positions
    And no exception should be thrown
    And the sequence value supplier will return "<sequenceValue>"
    When the next encrypted value is requested
    Then the exception message should contain "must not be blank"
    Examples:
      | sequenceValue |
      |               |

  Scenario: Decrypting an invalid value
    Given an FF31 sequence encryptor is created with key "secretkey16bytes", tweak "7bytes!", alphabet "0123456789", and padding to 8 positions
    And no exception should be thrown
    When the value "not-a-number" is decrypted
    Then the exception message should contain "Input character is missing from the alphabet"

  Scenario: Unsupported key size
    Given an FF31 sequence encryptor is created with key "key10bytes", tweak "7bytes!", alphabet "0123456789", and padding to 8 positions
    Then the exception message should contain "Key length not 128/192/256 bits"

  Scenario Outline: Encounter all-zeroes encryption result
    Given an FF31 sequence encryptor is created with key "secretkey16bytes", tweak "7bytes!", alphabet "0123456789", and padding to 6 positions
    And no exception should be thrown
    And the sequence value supplier will return "<sequenceNumber>"
    When the next encrypted value is requested
    And the exception message should contain "Sequence value must be different from the previous one"
    Examples:
      | sequenceNumber | comments                   |
      | 465579         | gets encrypted to '000000' |

  Scenario: Work around all-zeroes case
    Given an FF31 sequence encryptor is created with key "secretkey16bytes", tweak "7bytes!", alphabet "0123456789", and padding to 6 positions
    And no exception should be thrown
    # the first value gets encrypted to '000000', therefore the next value should be obtained
    And the sequence number supplier will return "465579" and then "465580"
    When the next encrypted value is requested
    Then no exception should be thrown
    Then the encrypted value should be "948434"

  Scenario: Too short custom alphabet
    Given an FF31 sequence encryptor is created with key "secretkey16bytes", tweak "7bytes!", alphabet "0abcde", and padding to 6 positions
    Then the exception message should contain "alphabet length must be between 10 and 256 (inclusive)"

  Scenario Outline: Encrypting and decrypting non-decimal values
    Given an FF31 sequence encryptor is created with key "secretkey16bytes", tweak "7bytes!", alphabet "01234abcdey", and padding to 6 positions
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
      | a             | 3a1ay4         | 00000a         |
      | ab            | 1ebc03         | 0000ab         |
      | abc           | e420b2         | 000abc         |
