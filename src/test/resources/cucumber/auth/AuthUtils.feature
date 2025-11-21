@security @utils #@disabled
Feature: Auth Utils

  Scenario: Access token checksum is calculated
    Given an access token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
    When the access token checksum is calculated
    Then the checksum should contain "sha256["
    And the checksum should contain "xxh64["
    And the checksum should not contain special characters
    And the checksum should equal "sha256[7f75367e7881255134e1375e723d1dea8ad5f6a4fdb79d938df1f1754a830606]_xxh64[74bf6ee34948fcda]"

  Scenario: Access token checksum is consistent
    Given an access token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
    When the access token checksum is calculated multiple times
    Then all checksums should be identical

  Scenario: Different access tokens have different checksums
    Given the following access tokens:
      | token                                                                                                                                                       |
      | eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c |
      | eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkphbmUgRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.cMErWtJ_4U1PatV0KTmUV8XGXdMw8NU_73zRFwBgSGU |
    When checksums are calculated for all tokens
    Then each checksum should be unique

  Scenario: Null access token causes exception
    When the access token checksum is calculated for a null token
    Then the exception message should contain "must not be blank"

  Scenario: Empty access token causes exception
    Given an access token ""
    When the access token checksum is calculated
    Then the exception message should contain "must not be blank"

  Scenario: Basic auth header is parsed
    Given a basic auth header "Basic dXNlcm5hbWU6cGFzc3dvcmQ="
    When the basic auth header is parsed
    Then the username should be "username"
    And the password should be "password"

  Scenario: Basic auth header with special characters is parsed
    Given a basic auth header "Basic dXNlckA6cGFzc3dvcmQjJA=="
    When the basic auth header is parsed
    Then the username should be "user@"
    And the password should be "password#$"

  Scenario: Invalid basic auth header format causes exception
    Given a basic auth header "Basic invalid-base64"
    When the basic auth header is parsed
    Then an exception should be thrown

  Scenario: Non-basic auth header causes exception
    Given a basic auth header "Bearer token"
    When the basic auth header is parsed
    Then the exception message should contain "has invalid prefix"

  Scenario: Null basic auth header causes exception
    When the basic auth header is parsed with a null header
    Then the exception message should contain "must not be null"

  Scenario: Basic auth header without separator causes exception
    Given a basic auth header "Basic bm9zZXBhcmF0b3I="
    When the basic auth header is parsed
    Then the exception message should contain "number of Basic Auth header value parts"
