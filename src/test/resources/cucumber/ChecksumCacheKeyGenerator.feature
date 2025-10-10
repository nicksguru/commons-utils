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

  Scenario Outline: Generating cache keys with different parameter types
    When a checksum cache key is generated with parameter of type "<paramType>"
    Then the generated key should equal "<expectedChecksum>"
    Examples:
      | paramType    | expectedChecksum                             |
      | String       | 1VecRt/MfxggcBPmW0Tky04sIpj0rEV7qPgnQ/Mekws= |
      | Integer      | c0dctApWjo2ooEXO0RATfhWfiQrE2og7axfcZRs6gEk= |
      | Double       | wHQN0lyd45ucjVq0Uui2m8wL+G8qYO1+Un550KMDWFI= |
      | Boolean      | tb6kG2xiP3wJ8b8k3K5Y66s8DN2QrZZrxDpFtEhn4Ss= |
      | List         | es5mBvmVa0bredV+BMD3/f0lFRwJX6U/hZAMcGezTow= |
      | Map          | tzRBPGROxJ9qfAfYiyZyRFgtZCLYnu6VVRH2s8DcsPI= |
      | CustomObject | oACJyTK51aLLC3PNjGgKk3myBfQ1hzx/s4Hav+IS7is= |
