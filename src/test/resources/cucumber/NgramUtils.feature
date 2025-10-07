@utils #@disabled
Feature: NgramUtils (with accented characters reduced to their ASCII equivalents)

  Scenario Outline: Create prefix ngrams
    Given input is "<input>"
    When prefix ngrams are created
    Then output should be "<item1>", "<item2>", "<item3>"
    Examples:
      | input | item1 | item2 | item3 |
      | TêSt  | tes   | test  |       |
      | tests | tes   | test  | tests |

  Scenario Outline: Create infix ngrams (actually trigrams)
    Given input is "<input>"
    When infix ngrams are created
    Then output should be "<item1>", "<item2>", "<item3>"
    Examples:
      | input | item1 | item2 | item3 |
      | TêSt  | est   |       |       |
      | tests | est   | sts   |       |

  Scenario Outline: Create all ngrams (longer prefix ngrams and infix trigrams)
    Given input is "<input>"
    When prefix and infix ngrams are created
    Then output should be "<item1>", "<item2>", "<item3>", "<item4>", "<item5>"
    Examples:
      | input | item1 | item2 | item3 | item4 | item5 |
      | tests | tes   | test  | tests | est   | sts   |
      | TêSt  | tes   | test  | est   |       |       |

  Scenario Outline: Russian morphology analysis
    Given input is "<input>"
    When prefix ngrams are created
    Then output should be "<item1>", "<item2>", "<item3>", "<item4>", "<item5>", "<item6>", "<item7>"
    Examples:
      | input   | item1 | item2 | item3 | item4  | item5   | item6  | item7   | comments                                 |
      | люди    | люд   | люди  | чел   | чело   | челов   | челове | человек | singular differs from plural drastically |
      | тест    | тес   | тест  | тесто |        |         |        |         | analyser mismatches 'тест' for 'тесто'   |
      | словАми | сло   | слов  | слова | словам | словами | слово  |         | different vowel in item6                 |
      | ёлка    | елк   | елка  |       |        |         |        |         | ё -> е                                   |
