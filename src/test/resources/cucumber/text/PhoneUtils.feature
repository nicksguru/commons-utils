@utils #@disabled
Feature: PhoneUtils

  Scenario Outline: Validate and normalize international phone numbers
    Given input is "<phone number>"
    When phone number is validated and normalized
    Then output should be "<normalized>"
    Examples:
      | phone number                                         | normalized   |
      | +1-555-123-4567                                      | +15551234567 |
      | Tel.: +1 (555) - 111 2222 -                          | +15551112222 |
      | Let's write something here: +1 (55 5) 1 2 3 1 2  3 1 | +15551231231 |
