@security @utils #@disabled
Feature: PEM Utils

  Scenario Outline: PEM format is fixed to be parseable
    Given a PEM string "<pem>"
    When the PEM is fixed
    Then the fixed PEM should be "<expected>"
    Examples:
      | pem                                                     | expected                                                    |
      | -----BEGIN PRIVATE KEY-----ABC                          | -----BEGIN PRIVATE KEY-----\nABC                            |
      | ABC-----END PRIVATE KEY-----                            | ABC\n-----END PRIVATE KEY-----                              |
      | -----BEGIN RSA PRIVATE KEY-----ABC                      | -----BEGIN RSA PRIVATE KEY-----\nABC                        |
      | ABC-----END RSA PRIVATE KEY-----                        | ABC\n-----END RSA PRIVATE KEY-----                          |
      | -----BEGIN PUBLIC KEY-----ABC                           | -----BEGIN PUBLIC KEY-----\nABC                             |
      | ABC-----END PUBLIC KEY-----                             | ABC\n-----END PUBLIC KEY-----                               |
      | -----BEGIN PRIVATE KEY-----\nABC                        | -----BEGIN PRIVATE KEY-----\nABC                            |
      | ABC\n-----END PRIVATE KEY-----                          | ABC\n-----END PRIVATE KEY-----                              |
      | -----BEGIN PRIVATE KEY-----ABC-----END PRIVATE KEY----- | -----BEGIN PRIVATE KEY-----\nABC\n-----END PRIVATE KEY----- |

  Scenario: Invalid PEM string causes exception
    Given a blank PEM string
    When the PEM is fixed
    Then an exception should be thrown

  Scenario: Private key is retrieved from key pair
    Given a valid key pair PEM
    When the private key is retrieved from the key pair
    Then a private key should be returned

  Scenario: Public key is retrieved from key pair
    Given a valid key pair PEM
    When the public key is retrieved from the key pair
    Then a public key should be returned

  Scenario: Public key is parsed from PEM
    Given a valid public key PEM
    When the public key is parsed
    Then a public key should be returned
