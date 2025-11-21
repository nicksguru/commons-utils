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
