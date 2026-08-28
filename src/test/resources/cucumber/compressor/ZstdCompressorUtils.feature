#@disabled
Feature: Zstd Compressor Service
  The service should compress and decompress data using Zstd algorithm
  And return the correct algorithm type

  Scenario Outline: Compressing data
    Given data to zstd-compress "<input>"
    When the data is compressed using Zstd
    Then no exception should be thrown
    And the zstd-compressed data should not be empty
    And the zstd-compressed data should be different from original
    And the zstd-compressed data should be decompressable back to original
    Examples:
      | input                 |
      | Hello World           |
      | This is a test string |
      | 123456789             |
      |                       |

  Scenario: Compressing null data
    Given data to zstd-compress is null
    When the data is compressed using Zstd
    Then the exception message should contain "null"

  Scenario: Decompressing invalid data
    Given invalid zstd-compressed data
    When the data is decompressed using Zstd
    Then an exception should be thrown

  Scenario: Decompressing a frame without embedded content size
    Given zstd-compressed data without content size
    When the data is decompressed using Zstd
    Then an exception should be thrown
    And the exception message should contain "unknown"

  Scenario: Decompressing a frame whose decompressed size exceeds the limit
    Given zstd-compressed data with decompressed size exceeding the limit
    When the data is decompressed using Zstd
    Then an exception should be thrown
    And the exception message should contain "exceeds"
