@security #@disabled
Feature: JwtUtils
  JWT Utils provides utility methods for retrieving and processing JWT components

  Scenario Outline: Retrieving user ID from JWT
    Given a JWT with subject "<subject>" and issuer "<issuer>"
    When the user ID is retrieved from the JWT
    Then no exception should be thrown
    And the user ID should be "<expectedUserId>"
    And the JWT provider should be "<expectedJwtProvider>"
    Examples:
      | subject | issuer                      | expectedUserId | expectedJwtProvider |
      | 123456  | https://accounts.google.com | google_123456  | GOOGLE              |
      | 789012  | https://other-issuer.com    | 789012         |                     |
      | abcdef  |                             | abcdef         |                     |

  Scenario: Retrieving user ID from JWT with empty subject
    Given a JWT with subject "" and issuer "https://accounts.google.com"
    When the user ID is retrieved from the JWT
    Then an exception should be thrown
    And the exception should be of type "BadJwtException"

  Scenario Outline: Retrieving username from JWT
    Given a JWT with the following claims:
      | claim              | value       |
      | preferred_username | <preferred> |
      | username           | <username>  |
    When the username is retrieved with default value "<default>"
    Then no exception should be thrown
    And the retrieved username should be "<expected>"
    Examples:
      | preferred | username  | default | expected  |
      | john.doe  |           | user123 | john.doe  |
      |           | jane.doe  | user456 | jane.doe  |
      | bob.smith | rob.smith | user789 | bob.smith |
      |           |           | user999 | user999   |

  Scenario Outline: Retrieving authorities from JWT
    Given a JWT with the following authorities configuration:
      | type            | value            |
      | <authorityType> | <authorityValue> |
    And JWT provider is "<jwtProvider>"
    When the authorities are retrieved from the JWT
    Then no exception should be thrown
    And the authorities should contain "<expectedAuthorities>"
    Examples:
      | authorityType  | authorityValue                                                                          | jwtProvider | expectedAuthorities |
      | keycloak       | {"realm_access":{"roles":["admin","user"]}}                                             |             | admin,user          |
      | keycloak       | {"resource_access":{"client1":{"roles":["reader","writer"]}}}                           |             | reader,writer       |
      | keycloak       | {"realm_access":{"roles":["admin"]},"resource_access":{"client1":{"roles":["writer"]}}} |             | admin,writer        |
      | cognito_groups | ["group1","group2"]                                                                     |             | group1,group2       |
      | keycloak       | {"realm_access":{"roles":["admin"]}}                                                    | GOOGLE      |                     |

  Scenario: Retrieving authorities from JWT with arbitrary values
    Given a JWT with the following authorities configuration:
      | type    | value |
      | invalid | 12345 |
    When the authorities are retrieved from the JWT
    Then no exception should be thrown

  Scenario Outline: Retrieving user locale from JWT
    Given a JWT with locale claim "<locale>"
    When the user locale is retrieved from the JWT
    Then no exception should be thrown
    And the retrieved locale should be "<expectedLocale>"
    Examples:
      | locale  | expectedLocale |
      | en-US   | en_US          |
      | fr-FR   | fr_FR          |
      | de      | de             |
      | invalid | en_US          |
      |         | en_US          |

  Scenario Outline: Retrieving AZP or AUD from JWT
    Given a JWT with the following claims:
      | claim | value |
      | azp   | <azp> |
      | aud   | <aud> |
    When the AZP or AUD is retrieved from the JWT
    Then no exception should be thrown
    And the retrieved values should contain "<expectedValues>"
    Examples:
      | azp                   | aud                    | expectedValues  |
      | client1               |                        | client1         |
      | ["client1","client2"] |                        | client1,client2 |
      |                       | client3                | client3         |
      |                       | ["client3","client4"]  | client3,client4 |
      | client5               | client6                | client5         |
      | ["client7","client8"] | ["client9","client10"] | client7,client8 |
