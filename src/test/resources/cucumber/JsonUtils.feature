@utils #@disabled
Feature: JsonUtils

  Scenario: Mask sensitive fields in JSON string
    Given input is "{\"username\":\"test\",\"password\":\"secret\",\"email\":\"test@example.com\",\"address\":\"123 Main St\",\"age\":30,\"dateOfBirth\":\"testDate\",\"isActive\":true,\"someNumber\":123, \"creditCardNumber\":\"1234567890\", \"passportNumber\":\"1234567890\",\"lastName\":\"Test\",\"phone\":\"555-123\"}"
    When sensitive JSON fields are masked
    Then masked JSON should contain "**MASKED**" for "username"
    And masked JSON should contain "**MASKED**" for "password"
    And masked JSON should contain "**MASKED**" for "email"
    And masked JSON should contain "30" for "age"
    And masked JSON should contain "**MASKED**" for "dateOfBirth"
    And masked JSON should contain "**MASKED**" for "creditCardNumber"
    And masked JSON should contain "**MASKED**" for "passportNumber"
    And masked JSON should contain "**MASKED**" for "lastName"
    And masked JSON should contain "**MASKED**" for "phone"
    And masked JSON should contain "true" for "isActive"
    And masked JSON should contain "123" for "someNumber"

  Scenario: Handle empty JSON input
    Given input is ""
    When sensitive JSON fields are masked
    Then masked JSON is empty
