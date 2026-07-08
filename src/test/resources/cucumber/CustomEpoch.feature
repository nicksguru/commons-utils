#@disabled
Feature: Custom epoch (time-sortable ID generation)

  Scenario Outline: Make ID time-sortable
    Given input is "<Sequence Number>"
    And date is "<Date>"
    When sequence number is converted to time-sortable
    Then output should be "<ID>"
    Examples:
      | Sequence Number         | Date                       | ID                     | Comment                               |
      | 0                       | 2024-08-24T00:00:00Z       | 0000000000             | minimum: 10 characters                |
      | 3                       | 2024-12-31T00:00:01.991Z   | 00N88R3ZE13            | 11 characters                         |
      | 10                      | 2024-12-31T00:00:00.999Z   | 00N88R1ZY3A            |                                       |
      | 10                      | 2024-12-31T00:00:01.999Z   | 00N88R3ZY37            |                                       |
      | 10                      | 2024-12-31T00:00:00.99987Z | 00N88R2003C            | rounded to .000, seconds bumped to 01 |
      | 10                      | 2024-12-31T00:00:00.00087Z | 00N88R00235            | rounded to .001                       |
      | 10                      | 2024-12-31T00:00:01.996Z   | 00N88R3ZR3C            |                                       |
      | 10                      | 2024-12-31T00:00:01.997Z   | 00N88R3ZT3D            |                                       |
      | 10                      | 2024-12-31T00:00:01.777Z   | 00N88R3HQ3D            |                                       |
      | 10                      | 2024-12-31T00:00:00.01Z    | 00N88R00M37            |                                       |
      | 10                      | 2024-12-31T00:00:00.001Z   | 00N88R00235            |                                       |
      | 10                      | 2024-12-31T00:00:00Z       | 00N88R00037            |                                       |
      | 32                      | 2025-03-01T12:34:56Z       | 00Z7E7000A0            |                                       |
      | 100                     | 2024-08-24T00:00:00Z       | 000000000ZB            |                                       |
      | 1024                    | 2025-09-01T00:00:00.001Z   | 01XEZR002A09           | 12th characters                       |
      | 32 768                  | 2025-12-01T00:00:00.017Z   | 02CEW0013A007          | 13th characters                       |
      | 1 048 576               | 2025-12-01T00:00:00.505Z   | 02CEW010AA0000         | 1 million: 14th characters            |
      | 1 050 123               | 2025-12-01T12:34:56.789Z   | 02CHMF1JGA0F3K         | see difference from value below       |
      | 1 050 124               | 2025-12-01T12:34:57.174Z   | 02CHMF2B4A0F3X         |                                       |
      | 1 051 234               | 2025-12-01T12:44:01.234Z   | 02CHNH2EZA0SYT         | see difference from previous value    |
      | 33 554 432              | 2026-01-01T00:00:00Z       | 02HJB8000A00007        | 33 million: 15th characters           |
      | 1 073 741 824           | 2028-01-01T00:00:00Z       | 069VYR000A000008       | 1 billion: 16th characters            |
      | 34 359 738 368          | 2030-01-01T00:00:00Z       | 0A2AV0000A0000007      | 34.3 billion: 16th characters         |
      | 1 099 511 627 776       | 2032-01-01T00:00:00Z       | 0DTMEG000A00000004     | 1 trillion: 18th characters           |
      | 35 184 372 088 832      | 2032-01-01T00:00:00Z       | 0DTMEG000A000000004    | 35.2 trillion: 19th characters        |
      | 922 337 203 685 477 579 | 2032-01-01T00:00:00Z       | 0DTMEG0007ZZZZZZZZZZZN | max. seq. (MaxLong / 10 - 1): 22 chrs |
      | 10 000                  | 2039-01-01T06:00:00Z       | 0V01YE00031N4          |                                       |
      | 10 000 000              | 2043-01-01T09:00:00Z       | 12GV390002ZBR88        | leftmost digit is 1                   |
      | 100 000 000             | 2060-01-01T00:00:00Z       | 22FZWR000XSNJG5        |                                       |
      | 10 000 000 000          | 2077-01-01T00:00:00Z       | 32FBY80002X47DT04      |                                       |
      | 1 000 000 000 000       | 2124-08-11T00:00:00Z       | 5W0TF8000931775801     | 100y                                  |
      | 10                      | 2224-08-24T00:00:00Z       | BR5Y200003C            | 200y                                  |
      | 10                      | 2568-08-24T00:00:00Z       | ZZ7DV00003B            | before overflow: 544y                 |

  Scenario Outline: Decode time-sortable ID
    Given input is "<ID>"
    When time-sortable ID is decoded
    Then parsed date should be "<Date>"
    And output should be "<Sequence Number>"
    Examples:
      | ID                      | Date                     | Sequence Number    | Comment                        |
      |                         |                          |                    | blank value                    |
      | !!!!!!!!!?              |                          |                    | value not in alphabet          |
      | 123456789               |                          |                    | value too short (< 10 chars)   |
      | 0000000000              | 2024-08-24T00:00:00Z     | 0                  |                                |
      | 00000000000000000000000 |                          |                    | more zeroes than 22 chars      |
      | 00N88R00235             | 2024-12-31T00:00:00.001Z | 10                 | .0008 becomes 0.001 (rounding) |
      | 00N88R0023F             |                          |                    | invalid checksum               |
      | O0N88R0023D             |                          |                    | not in alphabet ('O')          |
      | 00N88R00L3D             |                          |                    | not in alphabet ('L')          |
      | 00n88r00235             |                          |                    | not in alphabet (lowercase)    |
      | 000000000ZB             | 2024-08-24T00:00:00Z     | 100                |                                |
      | 02CEW010AA0000          | 2025-12-01T00:00:00.505Z | 1048576            |                                |
      | 069VYR000A000008        | 2028-01-01T00:00:00Z     | 1073741824         |                                |
      | ZZ7DV00007ZZZZZZZZZZZE  | 2568-08-24T00:00:00Z     | 922337203685477579 | max. sequence & timestamp      |
