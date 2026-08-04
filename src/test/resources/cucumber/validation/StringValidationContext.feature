@utils
Feature: StringValidationContext
  Validation of String values using StringValidationContext (created via ValiDsl.check)

  # 1. Constructor edge case
  Scenario: Blank field name is rejected by the constructor
    Given the string value is "hello"
    When the string is checked with a blank name
    Then the check should fail with message "name must not be blank"

  # 2. notNull()
  Scenario: notNull passes for a non-null string
    Given the string value is "hello"
    When the string is checked with notNull
    Then the check should pass

  Scenario: notNull fails for a null string
    Given the string value is null
    When the string is checked with notNull
    Then the check should fail with message "user.name must not be null"

  # 3. constraint(Predicate, String)
  Scenario: Custom predicate that fails throws the custom message
    Given the string value is "abc"
    When the string is checked with a failing custom predicate and message "must be uppercase"
    Then the check should fail with message "user.name must be uppercase"

  Scenario: Custom predicate that passes does not throw
    Given the string value is "ABC"
    When the string is checked with a passing custom predicate
    Then the check should pass

  Scenario: Custom predicate with a null message template uses the field name only
    Given the string value is "abc"
    When the string is checked with a custom predicate and a null message
    Then the check should fail with message "user.name"

  # 4. notEmpty()
  Scenario: notEmpty fails for null
    Given the string value is null
    When the string is checked with notEmpty
    Then the check should fail with message "user.name must not be null"

  Scenario: notEmpty fails for an empty string
    Given the string value is ""
    When the string is checked with notEmpty
    Then the check should fail with message "user.name must not be empty"

  Scenario: notEmpty passes for a whitespace-only string
    Given the string value is "   "
    When the string is checked with notEmpty
    Then the check should pass

  Scenario: notEmpty passes for a non-empty string
    Given the string value is "hello"
    When the string is checked with notEmpty
    Then the check should pass

  # 5. notBlank()
  Scenario: notBlank fails for null
    Given the string value is null
    When the string is checked with notBlank
    Then the check should fail with message "user.name must not be null"

  Scenario: notBlank fails for an empty string
    Given the string value is ""
    When the string is checked with notBlank
    Then the check should fail with message "user.name must not be blank"

  Scenario: notBlank fails for a whitespace-only string
    Given the string value is "   "
    When the string is checked with notBlank
    Then the check should fail with message "user.name must not be blank"

  Scenario: notBlank passes for a non-blank string
    Given the string value is "hello"
    When the string is checked with notBlank
    Then the check should pass

  # 6. shorterThan(int)
  Scenario Outline: shorterThan boundary conditions
    Given the string value is "<value>"
    When the string is checked with shorterThan 5
    Then the check <expectation>
    Examples:
      | value  | expectation                                                     |
      | null   | should fail with message "user.name must not be null"           |
      | abcd   | should pass                                                     |
      | abcde  | should fail with message "user.name length must be less than 5" |
      | abcdef | should fail with message "user.name length must be less than 5" |

  # 7. shorterThanOrEqual(int)
  Scenario Outline: shorterThanOrEqual boundary conditions
    Given the string value is "<value>"
    When the string is checked with shorterThanOrEqual 5
    Then the check <expectation>
    Examples:
      | value  | expectation                                                                 |
      | null   | should fail with message "user.name must not be null"                       |
      | abcd   | should pass                                                                 |
      | abcde  | should pass                                                                 |
      | abcdef | should fail with message "user.name length must be less than or equal to 5" |

  # 8. longerThan(int)
  Scenario Outline: longerThan boundary conditions
    Given the string value is "<value>"
    When the string is checked with longerThan 5
    Then the check <expectation>
    Examples:
      | value  | expectation                                                        |
      | null   | should fail with message "user.name must not be null"              |
      | abcdef | should pass                                                        |
      | abcde  | should fail with message "user.name length must be greater than 5" |
      | abcd   | should fail with message "user.name length must be greater than 5" |

  # 9. longerThanOrEqual(int)
  Scenario Outline: longerThanOrEqual boundary conditions
    Given the string value is "<value>"
    When the string is checked with longerThanOrEqual 5
    Then the check <expectation>
    Examples:
      | value  | expectation                                                                    |
      | null   | should fail with message "user.name must not be null"                          |
      | abcdef | should pass                                                                    |
      | abcde  | should pass                                                                    |
      | abcd   | should fail with message "user.name length must be greater than or equal to 5" |

  # 10. lengthBetweenInclusive(int, int)
  Scenario Outline: lengthBetweenInclusive boundary conditions
    Given the string value is "<value>"
    When the string is checked with lengthBetweenInclusive 2 and 5
    Then the check <expectation>
    Examples:
      | value  | expectation                                                                     |
      | null   | should fail with message "user.name must not be null"                           |
      | ab     | should pass                                                                     |
      | abcd   | should pass                                                                     |
      | abcde  | should pass                                                                     |
      | a      | should fail with message "user.name length must be between 2 and 5 (inclusive)" |
      | abcdef | should fail with message "user.name length must be between 2 and 5 (inclusive)" |

  # 11. startsWith(String)
  Scenario Outline: startsWith boundary conditions
    Given the string value is "<value>"
    When the string is checked with startsWith "Hello"
    Then the check <expectation>
    Examples:
      | value       | expectation                                                  |
      | null        | should fail with message "user.name must not be null"        |
      | Hello world | should pass                                                  |
      | Hi there    | should fail with message "user.name must start with 'Hello'" |
      | hello world | should fail with message "user.name must start with 'Hello'" |

  # 12. endsWith(String)
  Scenario Outline: endsWith boundary conditions
    Given the string value is "<value>"
    When the string is checked with endsWith "World"
    Then the check <expectation>
    Examples:
      | value       | expectation                                                |
      | null        | should fail with message "user.name must not be null"      |
      | hello World | should pass                                                |
      | hello world | should fail with message "user.name must end with 'World'" |
      | World hello | should fail with message "user.name must end with 'World'" |

  # 13. contains(String)
  Scenario Outline: contains boundary conditions
    Given the string value is "<value>"
    When the string is checked with contains "foo"
    Then the check <expectation>
    Examples:
      | value      | expectation                                             |
      | null       | should fail with message "user.name must not be null"   |
      | barfoo baz | should pass                                             |
      | bar baz    | should fail with message "user.name must contain 'foo'" |
      | barFOO baz | should fail with message "user.name must contain 'foo'" |

  # 14. Method chaining
  Scenario: Chained validations all pass without throwing
    Given the string value is "hello"
    When the string is checked with chained validations that all pass
    Then the check should pass

  Scenario: Chained validations short-circuit on the first failure
    Given the string value is null
    When the string is checked with chained validations where the first fails
    Then the check should fail with message "user.name must not be null"

  Scenario: Chained validations fail on the second check
    Given the string value is "hi"
    When the string is checked with chained validations where the second fails
    Then the check should fail with message "user.name length must be greater than or equal to 3"
