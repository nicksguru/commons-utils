@utils
Feature: RussianUtils

  Scenario Outline: Lemmatize Russian words
    Given Russian AOT library is available
    When Russian word "<input>" is lemmatized
    Then output should be "<lemma>"
    Examples:
      | input    | lemma   | comments                            |
      |          |         | empty input                         |
      | test     | test    | non-Russian word                    |
      | человек  | человек | singular noun                       |
      | человек  | человек | singular noun                       |
      | человека | человек | genitive case                       |
      | люди     | человек | plural noun                         |
      | люди,    | люди,   | unrecognized because of punctuation |
      | слов     | слово   | genitive plural                     |
      | слова    | слово   | nominative plural                   |
      | словами  | слово   | instrumental plural                 |
      | окне     | окно    | prepositional singular              |
      | теста    | тест    | genitive singular                   |
      | тесты    | тест    | nominative plural                   |
      | тестов   | тест    | genitive plural                     |
