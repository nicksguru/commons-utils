@utils #@disabled
Feature: TransformUtils

  Scenario: List is transformed with a single mapper
    Given a list of strings "apple,banana,cherry"
    When the list is transformed to uppercase using a single mapper
    Then the result should be a list containing "APPLE,BANANA,CHERRY"

  Scenario: List is transformed with two mappers
    Given a list of strings "apple,banana,cherry"
    When the list is transformed to uppercase and then reversed using two mappers
    Then the result should be a list containing "ELPPA,ANANAB,YRREHC"

  Scenario: List is transformed with three mappers
    Given a list of strings "apple,banana,cherry"
    When the list is transformed to uppercase, reversed, and then length calculated using three mappers
    Then the result should be a list containing "5,6,6"

  Scenario: Set is transformed with a single mapper
    Given a list of strings "apple,banana,cherry,apple"
    When the list is transformed to a set of uppercase strings using a single mapper
    Then the result should be a set containing "APPLE,BANANA,CHERRY"

  Scenario: Set is transformed with two mappers
    Given a list of strings "apple,banana,cherry,apple"
    When the list is transformed to a set using uppercase and then first letter extraction with two mappers
    Then the result should be a set containing "A,B,C"

  Scenario: Set is transformed with three mappers
    Given a list of strings "apple,banana,cherry,apple"
    When the list is transformed to a set using uppercase, first letter extraction, and ASCII code with three mappers
    Then the result should be a set containing "65,66,67"

  Scenario: Null collection is transformed to empty list
    When a null collection is transformed to a list
    Then the result should be an empty list

  Scenario: Null collection is transformed to empty set
    When a null collection is transformed to a set
    Then the result should be an empty set

  Scenario: Object is stringified
    Given the following objects:
      | type   | value       |
      | string | Hello World |
      | null   |             |
      | int    | 42          |
    When the objects are stringified
    Then the stringified results should be:
      | type   | result      |
      | string | Hello World |
      | null   | ''          |
      | int    | 42          |

  Scenario: Complex object is stringified
    Given a complex object
    When the complex object is stringified
    Then the result should be a JSON string
    And the JSON string should contain the object's properties

  Scenario: Object is stringified with pretty print
    Given a complex object
    When the complex object is stringified with pretty print
    Then the result should be a JSON string
    And the JSON string should contain line breaks

  Scenario Outline: Stream elements are mapped using the provided function
    Given stream elements "<input>"
    When the stream is transformed with mapping function "<function>"
    Then the transformed stream should contain "<expected>"
    Examples:
      | input        | function  | expected                   |
      | 1,2,3        | multiply2 | 2,4,6                      |
      | apple,banana | uppercase | APPLE,BANANA               |
      | 10,20,30     | divide2   | 5,10,15                    |
      | a,b,c        | addPrefix | prefix_a,prefix_b,prefix_c |

  Scenario: Stream elements are filtered based on predicate
    Given stream elements "1,2,3,4,5,6"
    When the stream is filtered with predicate "even"
    Then the transformed stream should contain "2,4,6"

  Scenario Outline: Stream elements are grouped by key function
    Given stream elements "<input>"
    When the stream is grouped by "<groupingFunction>"
    Then the grouped result should match "<expected>"
    Examples:
      | input                | groupingFunction | expected                     |
      | apple,banana,avocado | firstLetter      | a:[apple,avocado],b:[banana] |
      | 1,2,3,4,5,6          | evenOdd          | even:[2,4,6],odd:[1,3,5]     |
