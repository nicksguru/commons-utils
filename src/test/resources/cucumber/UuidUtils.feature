@utils #@disabled
Feature: UuidUtils

  Scenario: UUIDv7 is generated
    When a UUIDv7 is generated
    Then the UUID should be valid
    And the UUID should be of version 7

  Scenario: UUIDv4 is generated
    When a UUIDv4 is generated
    Then the UUID should be valid
    And the UUID should be of version 4

  Scenario: Certain UUID is encoded to Crockford Base32
    Given a UUID "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"
    When the UUID is encoded to Crockford Base32
    Then the encoded string should equal "z0emzbkxxg8x19v502gcj7kbyr"

  Scenario: Arbitrary UUID is encoded to Crockford Base32
    Given a UUID "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"
    When the UUID is encoded to Crockford Base32
    Then the encoded string should be 26 characters long
    And the encoded string should contain only valid Crockford Base32 characters

  Scenario: Multiple UUIDv7 are time-ordered
    When multiple UUIDv7 are generated with delays
    Then the UUIDs should be in ascending order when sorted lexicographically

  Scenario: UUID is parsed from string
    Given a UUID string "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"
    When the UUID string is parsed
    Then the parsed UUID should equal the original UUID string

  Scenario: Invalid UUID string is parsed
    Given an invalid UUID string "invalid-uuid"
    When the invalid UUID string is parsed
    Then the exception message should contain "Invalid UUID"

  Scenario: Crockford Base32 encoded UUIDs preserve sort order
    When multiple UUIDv7 are generated and encoded to Crockford Base32
    Then the encoded strings should be in the same order as the original UUIDs
