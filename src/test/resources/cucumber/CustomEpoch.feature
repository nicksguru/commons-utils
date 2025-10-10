#@disabled
Feature: Custom epoch (time-sortable ID generation)

  Scenario Outline: Make ID time-sortable
    Given input is "<Sequence Number>"
    And date is "<Date>"
    When sequence number is converted to time-sortable
    Then output should be "<ID>"
    Examples:
      | Sequence Number         | Date                       | ID                     | Comment                               |
      | 0                       | 2024-08-24T00:00:00Z       | 0000000005             | minimum: 10 characters                |
      | 3                       | 2024-12-31T00:00:01.991Z   | 00n88r3zey             |                                       |
      | 10                      | 2024-12-31T00:00:00.999Z   | 00n88r1zy3b            | 11th characters                       |
      | 10                      | 2024-12-31T00:00:01.999Z   | 00n88r3zy34            |                                       |
      | 10                      | 2024-12-31T00:00:00.99987Z | 00n88r2003d            | rounded to .000, seconds bumped to 01 |
      | 10                      | 2024-12-31T00:00:00.00087Z | 00n88r0023d            | rounded to .001                       |
      | 10                      | 2024-12-31T00:00:01.996Z   | 00n88r3zr34            |                                       |
      | 10                      | 2024-12-31T00:00:01.997Z   | 00n88r3zt36            |                                       |
      | 10                      | 2024-12-31T00:00:01.777Z   | 00n88r3hq36            |                                       |
      | 10                      | 2024-12-31T00:00:00.01Z    | 00n88r00m37            |                                       |
      | 10                      | 2024-12-31T00:00:00.001Z   | 00n88r0023d            |                                       |
      | 10                      | 2024-12-31T00:00:00Z       | 00n88r00035            |                                       |
      | 32                      | 2025-03-01T12:34:56Z       | 00z7e7000a5            |                                       |
      | 100                     | 2024-08-24T00:00:00Z       | 000000000zh            |                                       |
      | 1024                    | 2025-09-01T00:00:00.001Z   | 01xezr002a04           | 12th characters                       |
      | 32 768                  | 2025-12-01T00:00:00.017Z   | 02cew0013a007          | 13th characters                       |
      | 1 048 576               | 2025-12-01T00:00:00.505Z   | 02cew010aa0005         | 1 million: 14th characters            |
      | 1 050 123               | 2025-12-01T12:34:56.789Z   | 02chmf1jga0f3p         | see difference from value below       |
      | 1 050 124               | 2025-12-01T12:34:57.174Z   | 02chmf2b4a0f3y         |                                       |
      | 1 051 234               | 2025-12-01T12:44:01.234Z   | 02chnh2eza0syx         | see difference from previous value    |
      | 33 554 432              | 2026-01-01T00:00:00Z       | 02hjb8000a00004        | 33 million: 15th characters           |
      | 1 073 741 824           | 2028-01-01T00:00:00Z       | 069vyr000a000009       | 1 billion: 16th characters            |
      | 34 359 738 368          | 2030-01-01T00:00:00Z       | 0a2av0000a0000008      | 34.3 billion: 16th characters         |
      | 1 099 511 627 776       | 2032-01-01T00:00:00Z       | 0dtmeg000a00000001     | 1 trillion: 18th characters           |
      | 35 184 372 088 832      | 2032-01-01T00:00:00Z       | 0dtmeg000a000000005    | 35.2 trillion: 19th characters        |
      | 922 337 203 685 477 579 | 2032-01-01T00:00:00Z       | 0dtmeg0007zzzzzzzzzzzh | max. seq. (MaxLong / 10 - 1): 22 chrs |
      | 10 000                  | 2039-01-01T06:00:00Z       | 0v01ye00031n7          |                                       |
      | 10 000 000              | 2043-01-01T09:00:00Z       | 12gv390002zbr88        | leftmost digit is 1                   |
      | 100 000 000             | 2060-01-01T00:00:00Z       | 22fzwr000xsnjg3        |                                       |
      | 10 000 000 000          | 2077-01-01T00:00:00Z       | 32fby80002x47dt02      |                                       |
      | 1 000 000 000 000       | 2124-08-11T00:00:00Z       | 5w0tf8000931775808     | 100y                                  |
      | 10                      | 2224-08-24T00:00:00Z       | br5y2000036            | 200y                                  |
      | 10                      | 2568-08-24T00:00:00Z       | zz7dv00003a            | before overflow: 544y                 |

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
      | 0000000005              | 2024-08-24T00:00:00Z     | 0                  |                                |
      | 00000000000000000000000 |                          |                    | more zeroes than 22 chars      |
      | 00n88r0023d             | 2024-12-31T00:00:00.001Z | 10                 | .0008 becomes 0.001 (rounding) |
      | 00n88r0023f             |                          |                    | invalid checksum               |
      | o0n88r0023d             |                          |                    | not in alphabet ('o')          |
      | 00n88r00l3d             |                          |                    | not in alphabet ('l')          |
      | 00N88r0023d             |                          |                    | not in alphabet (uppercase)    |
      | 000000000zh             | 2024-08-24T00:00:00Z     | 100                |                                |
      | 02cew010aa0005          | 2025-12-01T00:00:00.505Z | 1048576            |                                |
      | 069vyr000a000009        | 2028-01-01T00:00:00Z     | 1073741824         |                                |
      | zz7dv00007zzzzzzzzzzze  | 2568-08-24T00:00:00Z     | 922337203685477579 | max. sequence value            |
      | zz7dv00003a             | 2568-08-24T00:00:00Z     | 10                 | max. timestamp                 |
