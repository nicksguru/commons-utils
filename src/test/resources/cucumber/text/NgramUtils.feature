@utils
Feature: NgramUtils (with accented characters reduced to their ASCII equivalents)

  Scenario Outline: Create 6-letter prefix ngrams with English morphology
    Given input is "<input>"
    When prefix ngrams are created
    Then output should be "<item1>", "<item2>", "<item3>", "<item4>"
    Examples:
      | input  | item1 | item2 | item3 | item4 | comments              |
      | TêSt   | tes   | test  |       |       |                       |
      | tests  | tes   | test  | tests |       |                       |
      | KEPT   | kep   | kept  | kee   | keep  | the lemma is 'keep'   |
      | feet   | fee   | feet  | foo   | foot  | the lemma is 'foot'   |
      | it was |       |       |       |       | stop words - no grams |
      | a      |       |       |       |       | stop word             |
      | it     |       |       |       |       | stop word             |
      | ran    | ran   | run   |       |       | irregular verb        |


  Scenario Outline: Create infix ngrams (actually trigrams)
    Given input is "<input>"
    When infix ngrams are created
    Then output should be "<item1>", "<item2>", "<item3>"
    Examples:
      | input | item1 | item2 | item3 |
      | TêSt  | est   |       |       |
      | tesTS | est   | sts   |       |

  Scenario Outline: Create all ngrams (6-letter prefix ngrams and 3-letter infix ones)
    Given input is "<input>"
    When prefix and infix ngrams are created
    Then output should be "<item1>", "<item2>", "<item3>", "<item4>", "<item5>"
    Examples:
      | input | item1 | item2 | item3 | item4 | item5 | comments                                |
      | tests | tes   | test  | tests | est   | sts   | prefix ngrams go first                  |
      | TêST  | tes   | test  | est   |       |       | both prefix and infix ngrams are sorted |

  Scenario Outline: Russian morphology analysis (no more than 6 letters in each prefix ngram)
    Given input is "<input>"
    When prefix ngrams are created
    Then output should be "<item1>", "<item2>", "<item3>", "<item4>", "<item5>", "<item6>"
    Examples:
      | input   | item1 | item2 | item3 | item4  | item5 | item6  | comments                                 |
      | люДИ    | люд   | люди  | чел   | чело   | челов | челове | singular differs from plural drastically |
      | ТЕсты   | тес   | тест  | тесты |        |       |        |                                          |
      | словАми | сло   | слов  | слова | словам | слово |        | different vowel in item5 (lemma)         |
      | Ёлка    | елк   | елка  |       |        |       |        | ё -> е                                   |

  Scenario Outline: Create ngrams from pre-tokenized unique words - equivalent to the String version
    Given input is "<input>"
    When ngrams are created from unique words in <mode> mode
    Then output should equal ngrams created from the input string
    And output should be "<item1>", "<item2>", "<item3>", "<item4>", "<item5>"
    Examples:
      | input | mode   | item1 | item2 | item3 | item4 | item5 | comments                                |
      | tests | PREFIX | tes   | test  | tests |       |       |                                         |
      | tests | INFIX  | est   | sts   |       |       |       |                                         |
      | tests | ALL    | tes   | test  | tests | est   | sts   | prefix ngrams go first                  |
      | TêST  | ALL    | tes   | test  | est   |       |       | both prefix and infix ngrams are sorted |
      | KEPT  | PREFIX | kep   | kept  | kee   | keep  |       | the lemma is 'keep'                     |
      | ran   | PREFIX | ran   | run   |       |       |       | irregular verb                          |
      | it was| PREFIX |       |       |       |       |       | stop words - no grams                   |

  Scenario Outline: Create ngrams from an empty words collection
    When ngrams are created from an empty words collection in <mode> mode
    Then output should be empty
    Examples:
      | mode   |
      | PREFIX |
      | INFIX  |
      | ALL    |
