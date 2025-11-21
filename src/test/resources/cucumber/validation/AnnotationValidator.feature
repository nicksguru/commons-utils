#@disabled
Feature: Annotation Validator
  Validates objects using annotations and checks nested properties

  Scenario Outline: getting class name for binding result
    Given an object of type "<className>"
    When the class name for binding result is retrieved
    Then the class name should be "<expectedResult>"
    Examples:
      | className           | expectedResult |
      | java.lang.String    | string         |
      | java.util.ArrayList | arrayList      |
      | java.util.HashMap   | hashMap        |

  Scenario: validating a valid object
    Given a valid test object
    When the object is validated
    Then no exception should be thrown

  Scenario: validating an invalid object
    Given an invalid test object
    When the object is validated
    Then ValidationException should be thrown

  Scenario: validating an object with invalid nested property
    Given a test object with invalid nested property
    When the object is validated
    Then ValidationException should be thrown

  Scenario: avoiding circular references during validation
    Given a test object with circular reference
    When the object is validated
    Then no exception should be thrown
