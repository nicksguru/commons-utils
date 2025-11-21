#@disabled
Feature: Zip Compressor Service
  The service should compress and decompress data using Zip algorithm
  And return the correct algorithm type

  Scenario Outline: Compressing data
    Given data to zip-compress "<input>"
    When the data is compressed using Zip
    Then no exception should be thrown
    And the zip-compressed data should not be empty
    And the zip-compressed data should be different from original
    And the zip-compressed data should be decompressable back to original
    Examples:
      | input                 |
      | Hello World           |
      | This is a test string |
      | 123456789             |
      |                       |

  Scenario: Compressing null data
    Given data to zip-compress is null
    When the data is compressed using Zip
    Then the exception message should contain "null"

  Scenario: Decompressing invalid data
    Given invalid zip-compressed data
    When the data is decompressed using Zip
    Then an exception should be thrown
