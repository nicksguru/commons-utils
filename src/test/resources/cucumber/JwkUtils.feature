@security #@disabled
Feature: JWK Utils
  Utility methods for handling JSON Web Keys (JWKs)

  Scenario: Finding smallest expiration date from JWK infos
    Given the following JWK infos with expiration dates:
      | authProviderId | expirationDate       |
      | provider1      | 2025-01-01T10:00:00Z |
      | provider2      | 2025-01-02T10:00:00Z |
      | provider3      | 2022-12-31T10:00:00Z |
    When the smallest expiration date is found
    Then the expiration date should be "2022-12-31T10:00:00Z"

  Scenario: Finding smallest expiration date when some dates are null
    Given the following JWK infos with expiration dates:
      | authProviderId | expirationDate       |
      | provider1      | 2025-01-01T10:00:00Z |
      | provider2      | null                 |
      | provider3      | 2022-12-31T10:00:00Z |
    When the smallest expiration date is found
    Then the expiration date should be "2022-12-31T10:00:00Z"

  Scenario: Finding smallest expiration date when all dates are null
    Given the following JWK infos with expiration dates:
      | authProviderId | expirationDate |
      | provider1      | null           |
      | provider2      | null           |
    When the smallest expiration date is found
    Then no expiration date should be found

  Scenario: Creating public keys from JWK info
    Given a JWK info with RSA keys
    When public keys are created from the JWK info
    Then the public keys should be created successfully
    And the number of public keys should be 2

  Scenario: Creating JWT decoder with RSA public key
    Given an RSA public key with algorithm "RSA"
    When a JWT decoder is created with the public key
    Then the JWT decoder should be created successfully

  Scenario Outline: Creating JWT decoder with different RSA algorithms
    Given an RSA public key with algorithm "<algorithm>"
    When a JWT decoder is created with the public key
    Then the JWT decoder should be created successfully
    Examples:
      | algorithm |
      | RSA       |
      | RS256     |
      | RS384     |
      | RS512     |

  Scenario: Fetching JWK set with cache control header
    Given a JWK set URL "https://example.com/.well-known/jwks.json"
    And an auth provider ID "test-provider"
    And the JWK set response has a cache control header with max age 3600
    When the JWK set is fetched
    Then the JWK info should have an expiration date
    And the JWK info should have auth provider ID "test-provider"

  Scenario: Fetching JWK set without cache control header
    Given a JWK set URL "https://example.com/.well-known/jwks.json"
    And an auth provider ID "test-provider"
    And the JWK set response has no cache control header
    When the JWK set is fetched
    Then the JWK info should not have an expiration date
    And the JWK info should have auth provider ID "test-provider"

  Scenario: Parsing PEM to JWK
    Given a PEM encoded random key pair
    When the PEM is parsed to JWK
    Then the JWK should be created successfully
