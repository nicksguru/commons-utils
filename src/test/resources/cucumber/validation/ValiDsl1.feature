@utils #@disabled
Feature: ValiDsl
  Validate various input values using various validation rules

  Scenario Outline: Test for notNull
    Given input is "<Value>"
    When validate: notNull
    Then success should be "<Success?>"
    Examples:
      | Value | Success? |
      |       | false    |
      | test  | true     |
      | 1     | true     |
      | 1.2   | true     |

  Scenario Outline: Test for notBlank
    Given input is "<Value>"
    When validate: notBlank
    Then success should be "<Success?>"
    Examples:
      | Value | Success? |
      |       | false    |
      | test  | true     |
      | 1     | true     |
      | 1.2   | true     |

  Scenario Outline: Test for positive
    Given input is "<Value>"
    When validate: positive
    Then success should be "<Success?>"
    Examples:
      | Value | Success? |
      |       | false    |
      | 1     | true     |
      | 1.2   | true     |
      | -2    | false    |
      | -2.3  | false    |

  Scenario Outline: Test for positiveOrZero
    Given input is "<Value>"
    When validate: positiveOrZero
    Then success should be "<Success?>"
    Examples:
      | Value | Success? |
      |       | false    |
      | 0     | true     |
      | 0.0   | true     |
      | 1     | true     |
      | 1.2   | true     |
      | -2    | false    |
      | -2.3  | false    |

  Scenario Outline: Test for lessThan
    Given integer1 is "<Value>"
    And integer2 is "<Other Value>"
    When validate: lessThan
    Then success should be "<Success?>"
    Examples:
      | Value | Other Value | Success? | Comment      |
      |       |             | false    | reject nulls |
      | 0     |             | false    | reject nulls |
      |       | 0           | false    | reject nulls |
      | 1     | 10          | true     |              |
      | 10    | 1           | false    |              |
      | 2     | 2           | false    |              |

  Scenario Outline: Test for lessThanOrEqual
    Given integer1 is "<Value>"
    And integer2 is "<Other Value>"
    When validate: lessThanOrEqual
    Then success should be "<Success?>"
    Examples:
      | Value | Other Value | Success? | Comment      |
      |       |             | false    | reject nulls |
      | 0     |             | false    | reject nulls |
      |       | 0           | false    | reject nulls |
      | 1     | 10          | true     |              |
      | 10    | 1           | false    |              |
      | 2     | 2           | true     |              |

  Scenario Outline: Test for greaterThan
    Given integer1 is "<Value>"
    And integer2 is "<Other Value>"
    When validate: greaterThan
    Then success should be "<Success?>"
    Examples:
      | Value | Other Value | Success? | Comment      |
      |       |             | false    | reject nulls |
      | 0     |             | false    | reject nulls |
      |       | 0           | false    | reject nulls |
      | 1     | 10          | false    |              |
      | 10    | 1           | true     |              |
      | 2     | 2           | false    |              |

  Scenario Outline: Test for greaterThanOrEqual
    Given integer1 is "<Value>"
    And integer2 is "<Other Value>"
    When validate: greaterThanOrEqual
    Then success should be "<Success?>"
    Examples:
      | Value | Other Value | Success? | Comment      |
      |       |             | false    | reject nulls |
      | 0     |             | false    | reject nulls |
      |       | 0           | false    | reject nulls |
      | 1     | 10          | false    |              |
      | 10    | 1           | true     |              |
      | 2     | 2           | true     |              |

  Scenario Outline: Test for betweenInclusive
    Given integer1 is "<Value>"
    And integer2 is "<Range Start>"
    And integer3 is "<Range End>"
    When validate: betweenInclusive
    Then success should be "<Success?>"
    Examples:
      | Value | Range Start | Range End | Success? | Comment      |
      |       |             |           | false    | reject nulls |
      | 0     |             | 0         | false    | reject nulls |
      |       |             | 0         | false    | reject nulls |
      | 1     | 10          |           | false    | reject nulls |
      | 5     | 5           | 5         | true     |              |
      | 5     | 1           | 5         | true     |              |
      | 5     | 5           | 7         | true     |              |
      | 5     | 6           | 7         | false    |              |
      | 5     | 2           | 4         | false    |              |

  Scenario Outline: Test for eq
    Given integer1 is "<Value>"
    And integer2 is "<Other Value>"
    When validate: eq
    Then success should be "<Success?>"
    Examples:
      | Value | Other Value | Success? | Comment                        |
      |       |             | true     | compare two nulls              |
      | 0     |             | false    | null isn't equal to any number |
      |       | 0           | false    | null isn't equal to any number |
      | 1     | 10          | false    |                                |
      | 10    | 1           | false    |                                |
      | 2     | 2           | true     |                                |
