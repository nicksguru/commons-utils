@cache #@disabled
Feature: ChecksumCacheKeyGenerator

  Scenario: Generating a cache key with string parameters
    When a checksum cache key is generated with string parameters "param1" and "param2"
    Then the generated key should contain the checksum of each parameter
    And the checksums should be joined with the topic delimiter

  Scenario: Generating a cache key with numeric parameters
    When a checksum cache key is generated with numeric parameters 123 and 456.78
    Then the generated key should contain the checksum of each parameter
    And the checksums should be joined with the topic delimiter

  Scenario: Generating a cache key with null parameters
    When a checksum cache key is generated with parameters "value" and null
    Then the generated key should contain the checksum of each parameter
    And the checksums should be joined with the topic delimiter

  Scenario: Generating a cache key with all null parameters
    When a checksum cache key is generated with parameters null and null
    Then the generated key should contain the checksum of each parameter
    And the checksums should be joined with the topic delimiter

  Scenario: Generating a cache key with complex objects
    And a complex object with nested properties
    When a checksum cache key is generated with the complex object as parameter
    Then the generated key should contain the JSON checksum of the object

  Scenario: Generating a cache key with no parameters
    When a checksum cache key is generated with no parameters
    Then the generated key should be an empty string

  Scenario: Generating different keys for different parameter orders
    When a checksum cache key is generated with parameters "A" and "B"
    And a checksum cache key is generated with parameters "B" and "A"
    Then the two generated keys should be different

  Scenario: Generating the same key for equal objects containing sets in different order
    When a checksum cache key is generated with an object containing a set in one order
    And a checksum cache key is generated with an equal object containing the same set in another order
    Then the two generated keys should be equal

  Scenario Outline: Generating cache keys with different parameter types
    When a checksum cache key is generated with parameter of type "<paramType>"
    Then the generated key should equal "<expectedKeyPart>"
    Examples:
      | paramType    | expectedKeyPart     |
      | String       | 3d5061310b23b3b9    |
      | Integer      | 1217cb28c0ef2191    |
      | Double       | be806d59f2d89101    |
      | Boolean      | 40bbd512750b7629    |
      | List         | 9ce810e56ce6e90d    |
      | Map          | 76f493969db4cf18    |
      | CustomObject | 495ad01a45199df6    |
