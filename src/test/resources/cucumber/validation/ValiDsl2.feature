@utils #@disabled
Feature: ValiDsl
  Validate various input values using various validation rules

  Scenario: Non-null string validation
    Given a string value "test"
    When the string is validated to be not null
    Then the validation should pass

  Scenario: Null string validation
    Given a null string value
    When the string is validated to be not null
    Then the validation should fail with message "user.name must not be null"

  Scenario: Non-blank string validation
    Given a string value "test"
    When the string is validated to be not blank
    Then the validation should pass

  Scenario: Blank string validation
    Given a string value ""
    When the string is validated to be not blank
    Then the validation should fail with message "user.name must not be blank"

  Scenario: String length validation
    Given a string value "test"
    When the string length is validated to be between 2 and 10
    Then the validation should pass

  Scenario: String too short validation
    Given a string value "a"
    When the string length is validated to be between 2 and 10
    Then the validation should fail with message "user.name length must be between 2 and 10 (inclusive)"

  Scenario: String with custom predicate validation
    Given a string value "test123"
    When the string is validated with a custom predicate for alphanumeric
    Then the validation should pass

  Scenario: String with custom predicate failure validation
    Given a string value "test@123"
    When the string is validated with a custom predicate for alphanumeric
    Then the validation should fail with message "user.name must be alphanumeric"

  Scenario: Positive integer validation
    Given an integer value 5
    When the integer is validated to be greater than 0
    Then the validation should pass

  Scenario: Non-positive integer validation
    Given an integer value 0
    When the integer is validated to be greater than 0
    Then the validation should fail with message "user.age must be greater than 0"

  Scenario: Integer range validation
    Given an integer value 5
    When the integer is validated to be between 1 and 10
    Then the validation should pass

  Scenario: Integer out of range validation
    Given an integer value 15
    When the integer is validated to be between 1 and 10
    Then the validation should fail with message "user.age must be between 1 and 10 (inclusive)"

  Scenario: Integer with multiple custom predicates validation
    Given an integer value 5
    When the integer is validated with multiple custom predicates
    Then the validation should pass

  Scenario: Integer with multiple custom predicates failure validation
    Given an integer value 15
    When the integer is validated with multiple custom predicates
    Then the validation should fail with message "user.age must be less than 10"

  Scenario: Positive long validation
    Given a long value 5
    When the long is validated to be greater than 0
    Then the validation should pass

  Scenario: Non-positive long validation
    Given a long value 0
    When the long is validated to be greater than 0
    Then the validation should fail with message "user.count must be greater than 0"

  Scenario: Double range validation
    Given a double value 5.5
    When the double is validated to be between 1.0 and 10.0
    Then the validation should pass

  Scenario: Double out of range validation
    Given a double value 15.5
    When the double is validated to be between 1.0 and 10.0
    Then the validation should fail with message "user.rate must be between 1.0 and 10.0 (inclusive)"

  Scenario: Non-empty collection validation
    Given a collection with items:
      | item |
      | one  |
      | two  |
    When the collection is validated to be not empty
    Then the validation should pass

  Scenario: Empty collection validation
    Given a collection with items:
      | item |
    When the collection is validated to be not empty
    Then the validation should fail with message "user.items must not be empty"

  Scenario: Collection size validation
    Given a collection with items:
      | item |
      | one  |
      | two  |
    When the collection size is validated to be between 1 and 3
    Then the validation should pass

  Scenario: Collection size too large validation
    Given a collection with items:
      | item  |
      | one   |
      | two   |
      | three |
      | four  |
    When the collection size is validated to be between 1 and 3
    Then the validation should fail with message "user.items size must be between 1 and 3 (inclusive)"

  Scenario: Generic object not null validation
    Given a generic object
    When the object is validated to be not null
    Then the validation should pass

  Scenario: Null generic object validation
    Given a null generic object
    When the object is validated to be not null
    Then the validation should fail with message "user.data must not be null"

  Scenario: Multiple validations chaining
    Given a string value "test"
    When validations for not null and minimum length are chained
    Then the validation should pass

  Scenario: Multiple validations chaining with failure
    Given a string value "a"
    When validations for not null and minimum length are chained
    Then the validation should fail with message "user.name length must be greater than or equal to 2"

  Scenario: Validated value retrieval after validation
    Given a string value "test"
    When the string is validated and the value is retrieved
    Then the returned value should be "test"

  Scenario: Custom error message validation
    Given a string value ""
    When the string is validated with custom error message
    Then the validation should fail with message "Custom error: user.name is empty"

  Scenario: Delegate validator validation
    Given a string value "test"
    When the string is validated with a delegate validator
    Then the validation should pass

  Scenario: Delegate validator failure validation
    Given a string value "a"
    When the string is validated with a delegate validator
    Then the validation should fail with message "user.name length must be greater than or equal to 2"

  Scenario: Complex chained validations
    Given a string value "test123"
    When the string is validated with complex chained validations
    Then the validation should pass

  Scenario: Complex chained validations failure
    Given a string value "t"
    When the string is validated with complex chained validations
    Then the validation should fail with message "user.name length must be greater than or equal to 2"

  Scenario Outline: Instant - 'before' validation
    Given an Instant named "<name>" with value "<value>"
    And other Instant value "<other>"
    When 'before' validation is performed
    Then <expectation> should be thrown
    Examples:
      | name      | value                | other                | expectation  |
      | eventTime | 2025-09-10T10:15:30Z | 2025-09-10T10:15:31Z | no exception |
      | eventTime | 2025-09-10T10:15:30Z | 2025-09-10T10:15:30Z | an exception |
      | eventTime | 2025-09-10T10:15:31Z | 2025-09-10T10:15:30Z | an exception |
      | eventTime | 2024-09-10T10:15:30Z |                      | an exception |

  Scenario Outline: Instant - 'before or equal' validation
    Given an Instant named "<name>" with value "<value>"
    And other Instant value "<other>"
    When 'before or equal' validation is performed
    Then <expectation> should be thrown
    Examples:
      | name      | value                | other                | expectation  |
      | eventTime | 2025-09-10T10:15:30Z | 2025-09-10T10:15:31Z | no exception |
      | eventTime | 2025-09-10T10:15:30Z | 2025-09-10T10:15:30Z | no exception |
      | eventTime | 2025-09-10T10:15:31Z | 2025-09-10T10:15:30Z | an exception |
      | eventTime | 2025-09-10T10:15:30Z |                      | an exception |

  Scenario Outline: Instant - 'after' validation
    Given an Instant named "<name>" with value "<value>"
    And other Instant value "<other>"
    When 'after' validation is performed
    Then <expectation> should be thrown
    Examples:
      | name      | value                | other                | expectation  |
      | eventTime | 2025-09-10T10:15:31Z | 2025-09-10T10:15:30Z | no exception |
      | eventTime | 2025-09-10T10:15:30Z | 2025-09-10T10:15:30Z | an exception |
      | eventTime | 2025-09-10T10:15:30Z | 2025-09-10T10:15:31Z | an exception |
      | eventTime | 2025-09-10T10:15:30Z |                      | an exception |

  Scenario Outline: Instant - 'after or equal' validation
    Given an Instant named "<name>" with value "<value>"
    And other Instant value "<other>"
    When 'after or equal' validation is performed
    Then <expectation> should be thrown
    Examples:
      | name      | value                | other                | expectation  |
      | eventTime | 2025-09-10T10:15:31Z | 2025-09-10T10:15:30Z | no exception |
      | eventTime | 2025-09-10T10:15:30Z | 2025-09-10T10:15:30Z | no exception |
      | eventTime | 2025-09-10T10:15:30Z | 2025-09-10T10:15:31Z | an exception |
      | eventTime | 2025-09-10T10:15:30Z |                      | an exception |
