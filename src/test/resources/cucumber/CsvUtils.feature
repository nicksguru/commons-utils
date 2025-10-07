@utils #@disabled
Feature: CsvUtils

  Scenario Outline: Parse CSV
    Given input is "<input>"
    And CSV is parsed
    Then parsed CSV should be "<firstName1>", "<lastName1>", <age1>, "<firstName2>", "<lastName2>", <age2>
    Examples:
      | input                                                          | firstName1 | lastName1 | age1 | firstName2 | lastName2 | age2 |
      | firstName,lastName,age\nTest1,Tester1,31\nTest2,Tester2,32     | Test1      | Tester1   | 31   | Test2      | Tester2   | 32   |
      | age,lastName,firstName\n31,Tester1,Test1\n32,Tester2,Test2     | Test1      | Tester1   | 31   | Test2      | Tester2   | 32   |
      | firstName,lastName,age\n\n\nTest1,Tester1,31\nTest2,Tester2,32 | Test1      | Tester1   | 31   | Test2      | Tester2   | 32   |
      | firstName,lastName,age\nTest1,,31\nTest2,,32                   | Test1      |           | 31   | Test2      |           | 32   |
      | firstName,age,lastName\nTest1,31,\nTest2,32,                   | Test1      |           | 31   | Test2      |           | 32   |
