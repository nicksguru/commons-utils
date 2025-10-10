@cache #@disabled
Feature: ToStringJoiningCacheKeyGenerator

  Scenario: Generating a cache key with string parameters
    When a cache key is generated with string parameters "param1" and "param2"
    Then the generated key should be "param1::param2"
    And no exception should be thrown

  Scenario: Generating a cache key with numeric parameters
    When a cache key is generated with numeric parameters 123 and 456.78
    Then the generated key should be "123::456.78"
    And no exception should be thrown

  Scenario: Generating a cache key with null parameters
    When a cache key is generated with parameters "value" and null
    Then the generated key should be "value::"
    And no exception should be thrown

  Scenario: Generating a cache key with all null parameters
    When a cache key is generated with parameters null and null
    Then the generated key should be "::"
    And no exception should be thrown

  Scenario: Generating a cache key with empty parameters
    When a cache key is generated with parameters "" and ""
    Then the generated key should be "::"
    And no exception should be thrown

  Scenario: Generating a cache key with object parameters
    When a custom object with toString returning "CustomObject"
    When a cache key is generated with parameters "prefix" and the custom object
    Then the generated key should be "prefix::CustomObject"
    And no exception should be thrown

  Scenario: Generating a cache key with no parameters
    When a cache key is generated with no parameters
    Then the generated key should be ""
    And no exception should be thrown

  Scenario Outline: Generating cache keys with different parameter combinations
    When a cache key is generated with parameters "<param1>" and "<param2>"
    Then the generated key should be "<expectedKey>"
    And no exception should be thrown
    Examples:
      | param1  | param2   | expectedKey     |
      | user    | 123      | user::123       |
      | product | active   | product::active |
      | order   |          | order::         |
      |         | customer | ::customer      |
      | a::b    | c::d     | a::b::c::d      |
