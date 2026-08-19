#@disabled
Feature: Business exception provider
  Error codes are mapped to business exceptions so that remote errors are seen as local exceptions.

  Scenario Outline: exception is created without a cause
    Given error code <errorCode> is used
    When the exception is created without a cause
    Then no exception should be thrown
    And a new exception of type <exceptionClass> should be created
    And the created exception cause should be null
    And the mapped exception class should be <exceptionClass>
    Examples:
      | errorCode           | exceptionClass         |
      | ENTITY_NOT_FOUND    | NotFoundException      |
      | DUPLICATE_ENTITY    | ConflictException      |
      | USER_ALREADY_EXISTS | AlreadyExistsException |

  Scenario Outline: exception is created with a cause
    Given error code <errorCode> is used
    When the exception is created with a cause
    Then no exception should be thrown
    And a new exception of type <exceptionClass> should be created
    And the created exception cause should be the original cause
    Examples:
      | errorCode           | exceptionClass         |
      | ENTITY_NOT_FOUND    | NotFoundException      |
      | DUPLICATE_ENTITY    | ConflictException      |
      | USER_ALREADY_EXISTS | AlreadyExistsException |

  Scenario: each exception creation returns a new instance
    Given error code ENTITY_NOT_FOUND is used
    When the exception is created 2 times
    Then no exception should be thrown
    And each created exception should be a new instance
