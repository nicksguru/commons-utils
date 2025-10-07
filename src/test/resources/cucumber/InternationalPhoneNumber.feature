#@disabled
Feature: International Phone Number Validation

  Scenario Outline: Validating international phone numbers
    Given a phone number "<phoneNumber>"
    When the phone number is validated
    Then the validation result should be <isValid>
    Examples:
      | phoneNumber     | isValid | comments                                                       |
      | +1-555-123-4567 | true    |                                                                |
      | +44 7911 123456 | true    |                                                                |
      | +61 4 1234 5678 | true    |                                                                |
      | 555-123-4567    | false   |                                                                |
      | abcdefg         | false   |                                                                |
      | +123            | false   |                                                                |
      | <null>          | true    | special value denoting null                                    |
      | <empty>         | false   | special value denoting an empty string                         |
      | <whitespaces>   | false   | special value denoting a string consisting of whitespaces only |
