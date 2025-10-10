@cache #@disabled
Feature: Caffeine Entry Expiration Condition Implementation

  Scenario: Using a concrete implementation with a Caffeine cache
    When a Caffeine builder is created with the implementation
    And a cache is built from the builder
    And a value is loaded into the cache
    Then the value should be available in the cache

  Scenario: Verifying expiration behavior
    When a Caffeine builder is created with the implementation
    And a cache is built from the builder
    And the cache is accessed after the expiration time
    Then the value should be expired from the cache
