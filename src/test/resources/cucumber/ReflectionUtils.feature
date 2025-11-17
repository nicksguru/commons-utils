@utils #@disabled
Feature: ReflectionUtils

  Scenario: Discover class hierarchy for org.springframework.security.core.userdetails.User
    Given class name is "org.springframework.security.core.userdetails.User"
    When class hierarchy is discovered
    Then class hierarchy length should be 5
    And class hierarchy should contain "org.springframework.security.core.userdetails.User" at index 0
    And class hierarchy should contain "org.springframework.security.core.CredentialsContainer" at index 1
    And class hierarchy should contain "org.springframework.security.core.userdetails.UserDetails" at index 2
    And class hierarchy should contain "java.io.Serializable" at index 3
    And class hierarchy should contain "java.lang.Object" at index 4

  Scenario: Discover class hierarchy for guru.nicks.commons.validation.AnnotationValidator
    Given class name is "guru.nicks.commons.validation.AnnotationValidator"
    When class hierarchy is discovered
    Then class hierarchy length should be 2
    And class hierarchy should contain "guru.nicks.commons.validation.AnnotationValidator" at index 0
    And class hierarchy should contain "java.lang.Object" at index 1

  Scenario Outline: Detecting scalar types
    When the isScalar method is called with <input>
    Then the result should be <expected>
    Examples:
      | input    | expected |
      | "string" | true     |
      | 42       | true     |
      | 3.14     | true     |
      | true     | true     |
      | false    | true     |
      | null     | true     |

  Scenario Outline: Detecting non-scalar types
    When the isScalar method is called with a <type>
    Then the result should be false
    Examples:
      | type          |
      | list          |
      | array         |
      | map           |
      | custom object |
      | collection    |
