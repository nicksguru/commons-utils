@utils #@disabled
Feature: TimeUtils

  Scenario Outline: Convert H:M:S duration to seconds
    Given input is "<Hours:Minutes:Seconds>"
    When H:M:S duration is converted to seconds
    Then output should be "<Seconds>"
    Examples:
      | Hours:Minutes:Seconds | Seconds |
      | 0:0:0                 | 0       |
      | 0:0:1                 | 1       |
      | 0:1:2                 | 62      |
      | 1:2:3                 | 3723    |
