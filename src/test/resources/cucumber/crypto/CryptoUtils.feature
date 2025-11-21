@security #@disabled
Feature: Crypto Service
  Tests for the CryptoService implementation

  Scenario: Base64 encoding and decoding
    Given a byte array with content "Hello World"
    When the byte array is encoded to Base64
    Then the encoded result should be "SGVsbG8gV29ybGQ="
    When the encoded string is decoded from Base64
    Then the decoded result should match the original byte array

  Scenario: HMAC SHA-512 calculation
    Given a byte array with content "Message to authenticate"
    And a secret key "secretKey123"
    When HMAC SHA-512 is calculated and encoded as Base64
    Then the HMAC result should be "vEG9KxtuiZPWwYJrdrcnKczMwzz0hR9XYfbnzRYQNZwu7vpXvARajorm1gj2I2aAfRXIw8G71L5IJUgHOPMYcw=="

  Scenario: RSA encryption and decryption
    Given a byte array with content "Secret message for RSA"
    And RSA key pair is available
    When the byte array is encrypted with RSA public key
    Then no exception should be thrown
    And the encrypted result should not be empty
    And the encrypted result should be different from the original
    When the encrypted result is decrypted with RSA private key
    Then the decrypted result should match the original byte array

  Scenario: AES encryption with null plain text
    Given a null byte array
    And a secret key "validKey"
    When the byte array is encrypted with AES
    Then the exception message should contain "plainText must not be null"

  Scenario: AES encryption with too short secret key (key derivation)
    Given a byte array with content "Valid content"
    And a secret key "test123"
    When the byte array is encrypted with AES
    Then no exception should be thrown

  Scenario Outline: AES encryption and decryption
    Given a byte array with content "<content>"
    And a secret key "<secret_key>"
    When the byte array is encrypted with AES
    Then no exception should be thrown
    And the encrypted result should not be empty
    And the encrypted result should be different from the original
    When the encrypted result is decrypted with AES
    Then the decrypted result should match the original byte array
    Examples:
      | content                 | secret_key       |
      | 1234567890              | 0123456789abcdef |
      | Simple text             | password12356789 |
      | Special chars: !@#$%^&* | complexKey!@#$+= |

  Scenario: RSA encryption with null public key
    Given a byte array with content "Valid content"
    And a null RSA public key
    When the byte array is encrypted with RSA public key
    Then the exception message should contain "publicKey"

  Scenario: RSA encryption with null plain text
    Given a null byte array
    And RSA key pair is available
    When the byte array is encrypted with RSA public key
    Then the exception message should contain "plainText"
