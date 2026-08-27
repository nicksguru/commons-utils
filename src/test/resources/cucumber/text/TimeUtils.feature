@utils #@disabled
Feature: TimeUtils

  Scenario Outline: Convert H:M:S duration to seconds
    Given input is "<Hours:Minutes:Seconds>"
    When H:M:S duration is converted to seconds
    Then output should be "<Seconds>"
    Examples:
      | Hours:Minutes:Seconds | Seconds        | Comment                                |
      | 0:0:0                 | 0              |                                        |
      | 0:0:1                 | 1              |                                        |
      | 0:1:2                 | 62             |                                        |
      | 1:2:3                 | 3723           |                                        |
      | 596523:0:0            | 2147482800     | largest hours still fitting int        |
      | 596524:0:0            | 2147486400     | first hours value overflowing int      |
      | 1000000:10:10         | 3600000610     | wrapped negative with int math         |
      | 2000000:0:0           | 7200000000     | far beyond int range                   |
