@utils #@disabled
Feature: TextUtils

  Scenario Outline: Parse comma-separated values
    Given input is "<input>"
    When comma-separated string is parsed
    Then output should be "<item1>", "<item2>", "<item3>"
    Examples:
      | input                   | item1 | item2 | item3 |
      | test                    | test  |       |       |
      | test1,TEst2,test1,test2 | test1 | TEst2 | test2 |
      | ,test1,test2,1-2-3      | test1 | test2 | 1-2-3 |
      | ,,test1,,test2,,test3,  | test1 | test2 | test3 |

  Scenario Outline: Split string into words
    Given input is "<input>"
    When string is split into words
    Then output should be "<item1>", "<item2>", "<item3>"
    Examples:
      | input                   | item1 | item2 | item3 |
      | test                    | test  |       |       |
      | test-test               | test  | test  |       |
      | test-TEst               | test  | test  |       |
      | test 12_3               | test  | 12    | 3     |
      | Têst,tèSt,tésT          | têst  | tèst  | tést  |
      | TEst1 teST2 tEst3       | test1 | test2 | test3 |
      | test,,test              | test  | test  |       |
      | test1_test2 test3       | test1 | test2 | test3 |
      | ,test1/test2,test1      | test1 | test2 | test1 |
      | ,tOst1[tÂst2`]tÒst1+%   | tost1 | tâst2 | tòst1 |
      | @@TEST1::tÎst2,:,tãst3& | test1 | tîst2 | tãst3 |

  Scenario Outline: Collect unique words, reducing accents to their base characters
    Given input is "<input>"
    When unique words are collected, reducing accented characters
    Then output should be "<item1>", "<item2>", "<item3>"
    Examples:
      | input                  | item1 | item2 | item3 |
      | test                   | test  |       |       |
      | ёлка                   | елка  |       |       |
      | test test              | test  |       |       |
      | test 12-3_12           | 12    | 3     | test  |
      | test TEst              | test  |       |       |
      | Têst,tèSt,tésT         | test  |       |       |
      | TEst1 teST2 tEst3      | test1 | test2 | test3 |
      | test,,test             | test  |       |       |
      | test1_test2 test3      | test1 | test2 | test3 |
      | ,test1/test2,test1     | test1 | test2 |       |
      | ,tOst1[tÂst2`tÒst1     | tast2 | tost1 |       |
      | @@TEST1::tÎst2,,tãst3, | tast3 | test1 | tist2 |

  Scenario Outline: Reduce accents to their base characters
    Given input is "<input>"
    When accented characters are reduced
    Then output should be "<output>"
    Examples:
      | input                        | output                       |
      | test                         | test                         |
      | test-à,è,ì,ò,ù,À,È,Ì,Ò,Ù     | test-a,e,i,o,u,A,E,I,O,U     |
      | test-á,é,í,ó,ú,ý,Á,É,Í,Ó,Ú,Ý | test-a,e,i,o,u,y,A,E,I,O,U,Y |
      | test-â,ê,î,ô,û,Â,Ê,Î,Ô,Û     | test-a,e,i,o,u,A,E,I,O,U     |
      | test-ã,ñ,õ,Ã,Ñ,Õ             | test-a,n,o,A,N,O             |
      | test-ёЁ                      | test-еЕ                      |

  Scenario Outline: Remove punctuation
    Given input is "<input>"
    When punctuation is removed
    Then output should be "<item1>", "<item2>", "<item3>"
    Examples:
      | input                                             | item1                               | item2 | item3 |
      | test                                              | test                                |       |       |
      | test1,test2                                       | test1 test2                         |       |       |
      | :#test1.@,+()test2~<test3`>_test4&=?test5%test6^! | test1 test2 test3 test4 test5 test6 |       |       |

  Scenario Outline: Detect magnitude of count
    Given input is "<count>"
    When magnitude of count is detected
    Then output should be "<magnitude>"
    Examples:
      | count       | magnitude                    |
      | -100        | negative                     |
      | -1          | negative                     |
      | 0           | zero                         |
      | 1           | several                      |
      | 5           | several                      |
      | 9           | several                      |
      | 10          | ten                          |
      | 11          | more than ten                |
      | 15          | more than ten                |
      | 19          | more than ten                |
      | 20          | tens of                      |
      | 50          | tens of                      |
      | 99          | tens of                      |
      | 100         | a hundred                    |
      | 101         | more than a hundred          |
      | 150         | more than a hundred          |
      | 199         | more than a hundred          |
      | 200         | hundreds of                  |
      | 500         | hundreds of                  |
      | 999         | hundreds of                  |
      | 1000        | a thousand                   |
      | 1001        | more than a thousand         |
      | 1500        | more than a thousand         |
      | 1999        | more than a thousand         |
      | 2000        | thousands of                 |
      | 9999        | thousands of                 |
      | 10000       | ten thousand                 |
      | 10001       | more than ten thousand       |
      | 10500       | more than ten thousand       |
      | 10999       | more than ten thousand       |
      | 11000       | tens of thousands            |
      | 99999       | tens of thousands            |
      | 100000      | a hundred thousand           |
      | 100001      | more than a hundred thousand |
      | 150000      | more than a hundred thousand |
      | 199999      | more than a hundred thousand |
      | 200000      | hundreds of thousands        |
      | 999999      | hundreds of thousands        |
      | 1000000     | a million                    |
      | 1000001     | more than a million          |
      | 1500000     | more than a million          |
      | 1999999     | more than a million          |
      | 2000000     | millions of                  |
      | 9999990     | millions of                  |
      | 10000000    | ten million                  |
      | 10000001    | more than ten million        |
      | 15000000    | more than ten million        |
      | 19999999    | more than ten million        |
      | 20000000    | tens of millions             |
      | 99999999    | tens of millions             |
      | 100000000   | a hundred million            |
      | 100000001   | more than a hundred million  |
      | 150000000   | more than a hundred million  |
      | 199999999   | more than a hundred million  |
      | 200000000   | hundreds of millions         |
      | 999999999   | hundreds of millions         |
      | 1000000000  | a billion                    |
      | 1000000001  | more than a billion          |
      | 1500000000  | more than a billion          |
      | 1999999999  | more than a billion          |
      | 2000000000  | billions of                  |
      | 10000000000 | billions of                  |
