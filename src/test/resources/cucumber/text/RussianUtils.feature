@utils
Feature: RussianUtils

  Scenario: Get lookupForMeanings method handle successfully
    Given Russian AOT library is available
    When lookupForMeanings method handle is requested
    Then method handle should not be null
    And no exception should be thrown

  Scenario: Get getLemma method handle successfully
    Given Russian AOT library is available
    When getLemma method handle is requested
    Then method handle should not be null
    And no exception should be thrown

  Scenario: Multiple calls to getLookupForMeaningsMethod return same handle
    Given Russian AOT library is available
    When lookupForMeanings method handle is requested twice
    Then both handles should be the same
    And no exception should be thrown

  Scenario: Multiple calls to getGetLemmaMethod return same handle
    Given Russian AOT library is available
    When getLemma method handle is requested twice
    Then both handles should be the same
    And no exception should be thrown

  Scenario Outline: Lemmatize Russian words
    Given Russian AOT library is available
    When Russian word "<input>" is lemmatized
    Then output should be "<lemma>"
    Examples:
      | input    | lemma   | comments               |
      | человек  | человек | singular noun          |
      | человека | человек | genitive case          |
      | люди     | человек | plural noun            |
      | слов     | слово   | genitive plural        |
      | слова    | слово   | nominative plural      |
      | словами  | слово   | instrumental plural    |
      | окне     | окно    | prepositional singular |
      | теста    | тест    | genitive singular      |
      | тесты    | тест    | nominative plural      |
      | тестов   | тест    | genitive plural        |
