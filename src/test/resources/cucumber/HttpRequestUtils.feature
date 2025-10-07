#@disabled
Feature: HTTP Request Utils

  Scenario Outline: HTTP status resolution for standard codes.
    When HTTP status code <code> is resolved
    Then resolution present is <present>
    And resolved status name should equal "<name>"
    And no exception should be thrown
    Examples:
      | code | present | name                  |
      | 200  | true    | OK                    |
      | 201  | true    | CREATED               |
      | 400  | true    | BAD_REQUEST           |
      | 401  | true    | UNAUTHORIZED          |
      | 404  | true    | NOT_FOUND             |
      | 500  | true    | INTERNAL_SERVER_ERROR |

  Scenario Outline: HTTP status resolution for special codes.
    When HTTP status code <code> is resolved
    Then resolution present is <present>
    And resolved status name should equal "<name>"
    Examples:
      | code | present | name                          |
      | 418  | true    | I_AM_A_TEAPOT                 |
      | 451  | true    | UNAVAILABLE_FOR_LEGAL_REASONS |
      | 102  | true    | PROCESSING                    |
      | 226  | true    | IM_USED                       |

  Scenario Outline: HTTP status resolution for invalid codes.
    When HTTP status code <code> is resolved
    Then resolution present is false
    And resolved status name should equal ""
    Examples:
      | code |
      | 999  |
      | 0    |
      | -1   |
      | 1000 |

  Scenario Outline: Caching behavior verification through multiple resolutions.
    When HTTP status code <code> is resolved
    And HTTP status code <code> is resolved
    Then resolution present is <present>
    And resolved status name should equal "<name>"
    Examples:
      | code | present | name |
      | 200  | true    | OK   |
      | 999  | false   |      |

  Scenario Outline: Accepted languages are parsed by priority and filtered by allowed locales.
    Given Accept-Language header is "<header>"
    And allowed locales are:
      | tag |
      | en  |
      | ja  |
      | ru  |
      | fr  |
    When Accept-Language header is parsed
    Then parsed locales should equal "<expectedTags>"
    And no exception should be thrown
    Examples:
      | header                     | expectedTags |
      | ja,en;q=0.4                | ja,en        |
      | en-US,en;q=0.9,ja;q=0.8    | en,ja        |
      | fr;q=0.8,ru;q=0.6,en;q=0.4 | fr,ru,en     |
      |                            |              |
      | zz,yy;q=0.2                |              |
      | de-DE,it;q=0.5             |              |

  Scenario Outline: Language quality values determine priority order.
    Given Accept-Language header is "<header>"
    And allowed locales are:
      | tag |
      | en  |
      | fr  |
      | de  |
    When Accept-Language header is parsed
    Then parsed locales should equal "<expectedTags>"
    Examples:
      | header                     | expectedTags |
      | en;q=0.5,fr;q=0.8,de;q=0.3 | fr,en,de     |
      | de,fr;q=0.9,en;q=0.1       | de,fr,en     |
      | fr;q=0.0,en;q=1.0          | en           |

  Scenario: Exception handling during header access.
    Given Accept-Language header access throws exception
    And allowed locales are:
      | tag |
      | en  |
      | fr  |
    When Accept-Language header is parsed
    Then parsed locales should equal ""
    And no exception should be thrown

  Scenario Outline: Empty and malformed headers are handled gracefully.
    Given Accept-Language header is "<header>"
    And allowed locales are:
      | tag |
      | en  |
      | ja  |
    When Accept-Language header is parsed
    Then parsed locales should equal "<expectedTags>"
    Examples:
      | header       | expectedTags |
      |              |              |
      | malformed;;; |              |
      | ;q=invalid   |              |

  Scenario Outline: Header is set only when non-blank values exist.
    Given the following header values are provided:
      | value |
      | <v1>  |
      | <v2>  |
      | <v3>  |
    When header "<headerName>" is set from provided values
    Then header should be set is <shouldBeSet>
    And no exception should be thrown
    Examples:
      | headerName    | v1   | v2    | v3 | shouldBeSet |
      | X-Test        | a    |       | c  | true        |
      | X-Test        |      |       |    | false       |
      | Content-Type  | text | html  |    | true        |
      | Cache-Control |      | blank |    | true        |
      | X-Empty       |      |       |    | false       |

  Scenario Outline: Header value composition with comma separation.
    Given the following header values are provided:
      | value    |
      | <value1> |
      | <value2> |
      | <value3> |
    When header "X-Composed" is set from provided values
    Then response header "X-Composed" should be set to "<expectedValue>"
    And no exception should be thrown
    Examples:
      | value1 | value2 | value3 | expectedValue      |
      | first  | second | third  | first,second,third |
      | only   |        |        | only               |
      | start  |        | end    | start,end          |

  Scenario Outline: Remote IP is resolved from known headers or remote address.
    Given request headers are configured:
      | name             | value     |
      | CF-Connecting-IP | <cfIp>    |
      | X-Real-IP        | <xRealIp> |
    And request remote address is "<remote>"
    When proxied remote IP is resolved
    Then resolved IP should equal "<expected>"
    And no exception should be thrown
    Examples:
      | cfIp    | xRealIp | remote    | expected  |
      | 1.1.1.1 | 2.2.2.2 | 9.9.9.9   | 1.1.1.1   |
      |         | 2.2.2.2 | 9.9.9.9   | 2.2.2.2   |
      | unknown | 2.2.2.2 | 9.9.9.9   | 2.2.2.2   |
      |         |         | 192.0.2.1 | 192.0.2.1 |
      | invalid | invalid | 10.0.0.1  | 10.0.0.1  |

  Scenario Outline: Header priority is respected in IP resolution.
    Given request headers are configured:
      | name             | value   |
      | CF-Connecting-IP | <cfIp>  |
      | X-Real-IP        | <xReal> |
    And request remote address is "fallback.ip"
    When proxied remote IP is resolved
    Then resolved IP should equal "<expected>"
    And request header "CF-Connecting-IP" should be accessed
    Examples:
      | cfIp        | xReal        | expected    |
      | 8.8.8.8     | 4.4.4.4      | 8.8.8.8     |
      | 203.0.113.1 | 198.51.100.1 | 203.0.113.1 |

  Scenario Outline: Invalid IP addresses are skipped.
    Given request headers are configured:
      | name             | value     |
      | CF-Connecting-IP | <cfValue> |
      | X-Real-IP        | <xValue>  |
    And request remote address is "<fallback>"
    When proxied remote IP is resolved
    Then resolved IP should equal "<expected>"
    Examples:
      | cfValue | xValue    | fallback   | expected   |
      | unknown | 127.0.0.1 | 10.0.0.1   | 127.0.0.1  |
      | not-ip  | unknown   | 172.16.0.1 | 172.16.0.1 |
      |         | not-ip    | hostname   | hostname   |
