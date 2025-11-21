#@disabled
Feature: KeycloakUtils role parsing
  Utility class for parsing Keycloak-specific JWT claims.

  Scenario Outline: Parsing roles from JWT
    Given a JWT is provided with the following roles:
      | realmRoles   | clientOneRoles   | clientTwoRoles   |
      | <realmRoles> | <clientOneRoles> | <clientTwoRoles> |
    When roles are parsed from the JWT
    Then the parsed roles should be "<expectedRoles>"
    And a set of roles is expected to be present: <rolesPresent>
    And no exception should be thrown
    Examples:
      | testCase                         | realmRoles   | clientOneRoles | clientTwoRoles | expectedRoles                          | rolesPresent |
      | Both realm and resource roles    | realm-role   | client-role-1  | client-role-2  | client-role-1,client-role-2,realm-role | true         |
      | Only realm roles                 | realm-role   |                |                | realm-role                             | true         |
      | Only resource roles              |              | client-role-1  | client-role-2  | client-role-1,client-role-2            | true         |
      | Duplicate roles                  | dup-role     | dup-role       |                | dup-role                               | true         |
      | Roles with blank and null values | role1,,role2 | role3, ,role4  |                | role1,role2,role3,role4                | true         |
      | No Keycloak role claims          |              |                |                |                                        | false        |

  Scenario: An exception is thrown when parsing a null JWT
    Given a null JWT is provided
    When roles are parsed from the JWT
    Then an exception should be thrown
