#@disabled
Feature: FPE Utils

  Scenario Outline: Creating an FF1 sequence encryptor with invalid arguments
    Given an FF1 sequence encryptor is created with the following arguments
      | zeroPadValueDigits   | key   | tweak   | supplierIsNull   |
      | <zeroPadValueDigits> | <key> | <tweak> | <supplierIsNull> |
    And no exception should be thrown
    When the encryptor is instantiated
    Then an exception should be thrown
    Examples:
      | zeroPadValueDigits | key              | tweak        | supplierIsNull |
      | 5                  | secretkey16bytes | secret-tweak | false          |
      | 8                  |                  | secret-tweak | false          |
      | 8                  | secretkey16bytes |              | false          |
      | 8                  | secretkey16bytes | secret-tweak | true           |

  Scenario Outline: Encrypting and decrypting sequence values
    Given an FF1 sequence encryptor is created with key "secretkey16bytes", tweak "secret-tweak", and padding of 8 digits
    And no exception should be thrown
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
    Given an FF1 sequence encryptor is created with key "secretkey16bytes", tweak "secret-tweak", and padding of 8 digits
    And no exception should be thrown
    And the sequence value supplier will return <sequenceValue>
    When the next encrypted value is requested
    Then an exception should be thrown
    Examples:
      | sequenceValue |
      | -1            |

  Scenario: Decrypting an invalid value
    Given an FF1 sequence encryptor is created with key "secretkey16bytes", tweak "secret-tweak", and padding of 6 digits
    And no exception should be thrown
    When the value "not-a-number" is decrypted
    Then an exception should not be thrown

  Scenario: Unsupported key size
    Given an FF1 sequence encryptor is created with key "secretkey", tweak "secret-tweak", and padding of 6 digits
    Then the exception message should contain "key size must be "

  Scenario Outline: Encounter all-zeroes encryption result
    Given an FF1 sequence encryptor is created with key "secretkey16bytes", tweak "secret-tweak", and padding of 6 digits
    And no exception should be thrown
    And the sequence value supplier will return <sequenceNumber>
    When the next encrypted value is requested
    And the exception message should contain "Sequence value must be different from the previous one"
    Examples:
      | sequenceNumber | comments                   |
      | 161626         | gets encrypted to '000000' |

  Scenario: Work around all-zeroes case
    Given an FF1 sequence encryptor is created with key "secretkey16bytes", tweak "secret-tweak", and padding of 6 digits
    And no exception should be thrown
    # the first value gets encrypted to '000000', therefore the next value should be obtained
    And the sequence number supplier will return 161626 and then 161627
    When the next encrypted value is requested
    Then no exception should be thrown
    Then the encrypted value should be "117653"
