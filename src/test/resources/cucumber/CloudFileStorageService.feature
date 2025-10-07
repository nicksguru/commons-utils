#@disabled
Feature: CloudFileStorageService

  Scenario: Saving a file to cloud storage
    Given a cloud file storage service for provider "AWS_S3"
    And a file with content "Hello, World!" and filename "test.txt"
    And content type "text/plain"
    And user ID "user123"
    When the file is saved to storage without metadata
    Then the saved file should have the same filename
    And the saved file should have the same content type
    And the saved file should have the same user ID
    And the saved file should have a checksum
    And no exception should be thrown

  Scenario: Saving a file with metadata
    Given a cloud file storage service for provider "AWS_S3"
    And a file with content "Test content" and filename "metadata-test.txt"
    And content type "text/plain"
    And user ID "user456"
    And metadata
      | key        | value       |
      | category   | test        |
      | department | engineering |
    When the file is saved to storage with metadata
    Then the saved file should have the same filename
    And the saved file should have the metadata
    And no exception should be thrown

  Scenario: Finding a file by filename
    Given a cloud file storage service for provider "AWS_S3"
    And a file with filename "findme.txt" exists in storage
    When the file is found by filename
    Then the file should be returned
    And no exception should be thrown

  Scenario: Finding a file by ID
    Given a cloud file storage service for provider "AWS_S3"
    And a file with ID "file-123" exists in storage
    When the file is found by ID
    Then the file should be returned
    And no exception should be thrown

  Scenario: Getting a file that doesn't exist
    Given a cloud file storage service for provider "AWS_S3"
    And a file with filename "nonexistent.txt" does not exist in storage
    When the file is requested by filename
    Then the exception message should contain "File not found"

  Scenario: Getting file content
    Given a cloud file storage service for provider "AWS_S3"
    And a file with ID "content-123" and content "File content" exists in storage
    When the file content is requested
    Then the content should match the original content
    And no exception should be thrown

  Scenario: Listing files in a directory
    Given a cloud file storage service for provider "AWS_S3"
    And files exist in directory "test-dir/"
      | filename           | contentType | userId  |
      | test-dir/file1.txt | text/plain  | user123 |
      | test-dir/file2.txt | text/plain  | user123 |
      | test-dir/file3.txt | text/plain  | user456 |
    When files are listed for directory "test-dir/"
    Then 3 files should be returned
    And the files should have the correct filenames
    And no exception should be thrown

  Scenario: Deleting a file
    Given a cloud file storage service for provider "AWS_S3"
    And a file with ID "delete-123" exists in storage
    When the file is deleted by ID
    Then the file should be removed from storage
    And no exception should be thrown

  Scenario: Computing file checksum
    Given a cloud file storage service for provider "AWS_S3"
    And a file with content "Checksum test content"
    When the checksum is computed
    Then the checksum should not be empty
    And no exception should be thrown

  Scenario Outline: Saving files with different content types
    Given a cloud file storage service for provider "AWS_S3"
    And a file with content "<content>" and filename "<filename>"
    And content type "<contentType>"
    And user ID "user789"
    When the file is saved to storage without metadata
    Then the saved file should have the same content type
    And no exception should be thrown
    Examples:
      | content             | filename   | contentType      |
      | Text content        | doc.txt    | text/plain       |
      | {\"key\":\"value\"} | data.json  | application/json |
      | <html></html>       | page.html  | text/html        |
      | Binary content      | image.png  | image/png        |
      | CSV data            | report.csv | text/csv         |
