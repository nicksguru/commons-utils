#@disabled
Feature: FPE Utils

  Scenario Outline: Creating an FF1 sequence encryptor with invalid arguments
    Given an FF1 sequence encryptor is created with the following arguments
      | zeroPadValueDigits   | key   | tweak   | supplierIsNull   |
      | <zeroPadValueDigits> | <key> | <tweak> | <supplierIsNull> |
    When the encryptor is instantiated
    Then an exception should be thrown
    Examples:
      | zeroPadValueDigits | key                              | tweak        | supplierIsNull |
      | 5                  | ABCDEF0123456789ABCDEF0123456789 | secret-tweak | false          |
      | 8                  |                                  | secret-tweak | false          |
      | 8                  | ABCDEF0123456789ABCDEF0123456789 |              | false          |
      | 8                  | ABCDEF0123456789ABCDEF0123456789 | secret-tweak | true           |

  Scenario Outline: Encrypting and decrypting sequence values
    Given an FF1 sequence encryptor is created with key "ABCDEF0123456789ABCDEF0123456789", tweak "secret-tweak-456", and padding of 8 digits
    And the sequence value supplier will return <sequenceValue>
    When the next encrypted value is requested
    Then the encrypted value is not blank
    And decrypting the value returns <sequenceValue>
    And no exception should be thrown
    Examples:
      | sequenceValue |
      | 1             |
      | 12345         |
      | 99999999      |

  Scenario Outline: Getting the next encrypted value with an invalid sequence value
    Given an FF1 sequence encryptor is created with key "ABCDEF0123456789ABCDEF0123456789", tweak "secret-tweak-456", and padding of 8 digits
    And the sequence value supplier will return <sequenceValue>
    When the next encrypted value is requested
    Then an exception should be thrown
    Examples:
      | sequenceValue |
      | 0             |
      | -1            |

  Scenario: Decrypting an invalid value
    Given an FF1 sequence encryptor is created with key "ABCDEF0123456789ABCDEF0123456789", tweak "secret-tweak-456", and padding of 8 digits
    When the value "not-a-number" is decrypted
    Then an exception should not be thrown
