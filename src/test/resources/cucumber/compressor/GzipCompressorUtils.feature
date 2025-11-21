#@disabled
Feature: Gzip Compressor Service
  The service should compress and decompress data using Gzip algorithm
  And return the correct algorithm type

  Scenario Outline: Compressing data
    Given data to gzip-compress "<input>"
    When the data is compressed using Gzip
    Then no exception should be thrown
    And the gzip-compressed data should not be empty
    And the gzip-compressed data should be different from original
    And the gzip-compressed data should be decompressable back to original
    Examples:
      | input                 |
      | Hello World           |
      | This is a test string |
      | 123456789             |
      |                       |

  Scenario: Compressing null data
    Given data to gzip-compress is null
    When the data is compressed using Gzip
    Then the exception message should contain "null"

  Scenario: Decompressing invalid data
    Given invalid gzip-compressed data
    When the data is decompressed using Gzip
    Then an exception should be thrown
