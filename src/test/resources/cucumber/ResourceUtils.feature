@utils
Feature: ResourceUtils

  Scenario: Get build tag from Git commit ID
    Given application context has GitProperties with commit ID "abc123def456"
    When app build tag is retrieved
    Then build tag should be a checksum of "abc123def456"
    And build tag should be cached

  Scenario: Get build tag from build time when Git commit ID is not available
    Given application context has no GitProperties bean
    And application context has BuildProperties with time "2024-01-15T10:30:00Z"
    When app build tag is retrieved
    Then build tag should be a checksum of "2024-01-15T10:30:00Z"
    And build tag should be cached

  Scenario: Get build tag from current time when no build info is available
    Given application context has no GitProperties bean
    And application context has no BuildProperties bean
    When app build tag is retrieved
    Then build tag should be a checksum of current time
    And build tag should be cached

  Scenario: Get build tag from build time when Git commit ID is empty
    Given application context has GitProperties with empty commit ID
    And application context has BuildProperties with time "2024-02-20T14:45:00Z"
    When app build tag is retrieved
    Then build tag should be a checksum of "2024-02-20T14:45:00Z"
    And build tag should be cached

  Scenario: Get build tag from current time when build time is null
    Given application context has GitProperties with commit ID "xyz789"
    And application context has BuildProperties with null time
    When app build tag is retrieved
    Then build tag should be a checksum of "xyz789"
    And build tag should be cached

  Scenario: Build tag is cached and reused on subsequent calls
    Given application context has GitProperties with commit ID "cached123"
    When app build tag is retrieved first time
    And app build tag is retrieved second time
    Then both build tags should be identical

  Scenario: Concurrent cache misses don't serialize behind a global monitor
    When 8 threads concurrently retrieve and cache resource "/cucumber" and "ResourceUtils.feature"
    Then all concurrent retrievals should return the same cached entry

  Scenario: Cached and non-cached lookups return the same resource
    When resource "/cucumber" and "ResourceUtils.feature" is retrieved without cache
    And resource "/cucumber" and "ResourceUtils.feature" is retrieved and cached
    Then both lookups should return the same content

  Scenario: Windows-style separators normalize to the same cache key as forward slashes
    When resource "/cucumber" and "retry\RetryUtils.feature" is retrieved and cached
    Then the resource should be found
    And the cache should contain an entry for "/cucumber/retry/RetryUtils.feature"

  Scenario: Filename traversing above the root resolves to nothing in the cached lookup
    When resource "/cucumber" and "../../outside.txt" is retrieved and cached
    Then the resource should not be found
