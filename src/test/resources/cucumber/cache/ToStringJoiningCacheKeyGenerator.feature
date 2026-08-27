@cache #@disabled
Feature: ToStringJoiningCacheKeyGenerator

  Scenario: Generating a cache key with string parameters
    When a cache key is generated with string parameters "param1" and "param2"
    Then the generated key should be "2|6:param16:param2"
    And no exception should be thrown

  Scenario: Generating a cache key with numeric parameters
    When a cache key is generated with numeric parameters 123 and 456.78
    Then the generated key should be "2|3:1236:456.78"
    And no exception should be thrown

  Scenario: Generating a cache key with null parameters
    When a cache key is generated with parameters "value" and null
    Then the generated key should be "2|5:value4:null"
    And no exception should be thrown

  Scenario: Generating a cache key with all null parameters
    When a cache key is generated with parameters null and null
    Then the generated key should be "2|4:null4:null"
    And no exception should be thrown

  Scenario: Generating a cache key with empty parameters
    When a cache key is generated with string parameters "" and ""
    Then the generated key should be "2|0:0:"
    And no exception should be thrown

  Scenario: Generating a cache key with object parameters
    When a custom object with toString returning "CustomObject"
    When a cache key is generated with parameters "prefix" and the custom object
    Then the generated key should be "2|6:prefix12:CustomObject"
    And no exception should be thrown

  Scenario: Generating a cache key with no parameters
    When a cache key is generated with no parameters
    Then the generated key should be "0|"
    And no exception should be thrown

  Scenario Outline: Generating cache keys with different parameter combinations
    When a cache key is generated with string parameters "<param1>" and "<param2>"
    Then the generated key should be "<expectedKey>"
    And no exception should be thrown
    Examples:
      | param1  | param2   | expectedKey          |
      | user    | 123      | 2\|4:user3:123       |
      | product | active   | 2\|7:product6:active |
      | order   |          | 2\|5:order0:         |
      |         | customer | 2\|0:8:customer      |
      | a::b    | c::d     | 2\|4:a::b4:c::d      |

  Scenario: Colliding argument tuples produce distinct keys
    When a first cache key is generated with string parameters "a::b" and "c"
    And a second cache key is generated with string parameters "a" and "b::c"
    Then the two generated keys should not collide

  Scenario: Null and empty string arguments produce distinct keys
    When a first cache key is generated with parameters null and null
    And a second cache key is generated with string parameters "" and ""
    Then the two generated keys should not collide
