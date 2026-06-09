@utils
Feature: EnglishUtils

  Scenario Outline: Check if a word is a stop word
    Given input is "<input>"
    When stop word check is performed
    Then output should be "<result>"
    Examples:
      | input   | result | comments              |
      | the     | true   | definite article      |
      | a       | true   | indefinite article    |
      | an      | true   | indefinite article    |
      | and     | true   | conjunction           |
      | or      | true   | conjunction           |
      | but     | true   | conjunction           |
      | if      | true   | conjunction           |
      | while   | true   | conjunction           |
      | in      | true   | preposition           |
      | on      | true   | preposition           |
      | at      | true   | preposition           |
      | to      | true   | preposition           |
      | for     | true   | preposition           |
      | of      | true   | preposition           |
      | with    | true   | preposition           |
      | by      | true   | preposition           |
      | from    | true   | preposition           |
      | as      | true   | preposition           |
      | into    | true   | preposition           |
      | is      | true   | verb to be            |
      | are     | true   | verb to be            |
      | was     | true   | verb to be            |
      | were    | true   | verb to be            |
      | be      | true   | verb to be            |
      | been    | true   | verb to be            |
      | being   | true   | verb to be            |
      | have    | true   | auxiliary verb        |
      | has     | true   | auxiliary verb        |
      | had     | true   | auxiliary verb        |
      | do      | true   | auxiliary verb        |
      | does    | true   | auxiliary verb        |
      | did     | true   | auxiliary verb        |
      | this    | true   | demonstrative pronoun |
      | that    | true   | demonstrative pronoun |
      | these   | true   | demonstrative pronoun |
      | those   | true   | demonstrative pronoun |
      | i       | true   | personal pronoun      |
      | you     | true   | personal pronoun      |
      | he      | true   | personal pronoun      |
      | she     | true   | personal pronoun      |
      | it      | true   | personal pronoun      |
      | we      | true   | personal pronoun      |
      | they    | true   | personal pronoun      |
      | me      | true   | personal pronoun      |
      | him     | true   | personal pronoun      |
      | her     | true   | personal pronoun      |
      | us      | true   | personal pronoun      |
      | them    | true   | personal pronoun      |
      | my      | true   | possessive pronoun    |
      | your    | true   | possessive pronoun    |
      | his     | true   | possessive pronoun    |
      | its     | true   | possessive pronoun    |
      | our     | true   | possessive pronoun    |
      | their   | true   | possessive pronoun    |
      | cat     | false  | regular noun          |
      | dog     | false  | regular noun          |
      | run     | false  | regular verb          |
      | happy   | false  | adjective             |
      | quickly | false  | adverb                |

  Scenario Outline: Lemmatize irregular verbs (past tense to infinitive)
    Given input is "<input>"
    When word is lemmatized
    Then output should be "<lemma>"
    Examples:
      | input       | lemma      | comments                                |
      | was         | be         | past of be                              |
      | was,        | was,       | unrecognized because of punctuation     |
      | were        | be         | past of be                              |
      | am          | be         | present of be                           |
      | is          | be         | present of be                           |
      | are         | be         | present of be                           |
      | been        | be         | participle of be                        |
      | had         | have       | past of have                            |
      | has         | have       | present of have                         |
      | did         | do         | past of do                              |
      | does        | doe        | present of do (stemmer behavior)        |
      | done        | do         | participle of do                        |
      | went        | go         | past of go                              |
      | gone        | go         | participle of go                        |
      | goes        | goe        | 3rd person of go (stemmer behavior)     |
      | ran         | run        | past of run                             |
      | runs        | run        | 3rd person of run                       |
      | ate         | eat        | past of eat                             |
      | eaten       | eat        | participle of eat                       |
      | eats        | eat        | 3rd person of eat                       |
      | saw         | see        | past of see                             |
      | seen        | see        | participle of see                       |
      | sees        | see        | 3rd person of see                       |
      | came        | come       | past of come                            |
      | comes       | come       | 3rd person of come                      |
      | took        | take       | past of take                            |
      | taken       | take       | participle of take                      |
      | takes       | take       | 3rd person of take                      |
      | made        | make       | past of make                            |
      | makes       | make       | 3rd person of make                      |
      | gave        | give       | past of give                            |
      | given       | give       | participle of give                      |
      | gives       | give       | 3rd person of give                      |
      | knew        | know       | past of know                            |
      | known       | know       | participle of know                      |
      | knows       | know       | 3rd person of know                      |
      | got         | get        | past of get                             |
      | gotten      | get        | participle of get                       |
      | gets        | get        | 3rd person of get                       |
      | found       | find       | past of find                            |
      | finds       | find       | 3rd person of find                      |
      | thought     | think      | past of think                           |
      | thinks      | think      | 3rd person of think                     |
      | told        | tell       | past of tell                            |
      | tells       | tell       | 3rd person of tell                      |
      | became      | becam      | past of become (stemmer behavior)       |
      | becomes     | becom      | 3rd person of become (stemmer behavior) |
      | left        | leave      | past of leave                           |
      | leaves      | leav       | 3rd person of leave (stemmer behavior)  |
      | felt        | feel       | past of feel                            |
      | feels       | feel       | 3rd person of feel                      |
      | brought     | bring      | past of bring                           |
      | brings      | bring      | 3rd person of bring                     |
      | began       | begin      | past of begin                           |
      | begun       | begin      | participle of begin                     |
      | begins      | begin      | 3rd person of begin                     |
      | kept        | keep       | past of keep                            |
      | keeps       | keep       | 3rd person of keep                      |
      | held        | hold       | past of hold                            |
      | holds       | hold       | 3rd person of hold                      |
      | wrote       | write      | past of write                           |
      | written     | write      | participle of write                     |
      | writes      | write      | 3rd person of write                     |
      | stood       | stand      | past of stand                           |
      | stands      | stand      | 3rd person of stand                     |
      | heard       | hear       | past of hear                            |
      | hears       | hear       | 3rd person of hear                      |
      | meant       | mean       | past of mean                            |
      | means       | mean       | 3rd person of mean                      |
      | set         | set        | past of set                             |
      | sets        | set        | 3rd person of set                       |
      | met         | meet       | past of meet                            |
      | meets       | meet       | 3rd person of meet                      |
      | paid        | pay        | past of pay                             |
      | pays        | pay        | 3rd person of pay                       |
      | sat         | sit        | past of sit                             |
      | sits        | sit        | 3rd person of sit                       |
      | spoke       | speak      | past of speak                           |
      | spoken      | speak      | participle of speak                     |
      | speaks      | speak      | 3rd person of speak                     |
      | led         | lead       | past of lead                            |
      | leads       | lead       | 3rd person of lead                      |
      | read        | read       | past of read                            |
      | reads       | read       | 3rd person of read                      |
      | grew        | grow       | past of grow                            |
      | grown       | grow       | participle of grow                      |
      | grows       | grow       | 3rd person of grow                      |
      | lost        | lose       | past of lose                            |
      | loses       | lose       | 3rd person of lose                      |
      | fell        | fall       | past of fall                            |
      | fallen      | fall       | participle of fall                      |
      | falls       | fall       | 3rd person of fall                      |
      | sent        | send       | past of send                            |
      | sends       | send       | 3rd person of send                      |
      | built       | build      | past of build                           |
      | builds      | build      | 3rd person of build                     |
      | understood  | understand | past of understand                      |
      | understands | understand | 3rd person of understand                |
      | cut         | cut        | past of cut                             |
      | cuts        | cut        | 3rd person of cut                       |
      | put         | put        | past of put                             |
      | puts        | put        | 3rd person of put                       |
      | hit         | hit        | past of hit                             |
      | hits        | hit        | 3rd person of hit                       |
      | bought      | buy        | past of buy                             |
      | buys        | buy        | 3rd person of buy                       |
      | caught      | catch      | past of catch                           |
      | catches     | catch      | 3rd person of catch                     |
      | drew        | draw       | past of draw                            |
      | drawn       | draw       | participle of draw                      |
      | draws       | draw       | 3rd person of draw                      |
      | drove       | drive      | past of drive                           |
      | driven      | drive      | participle of drive                     |
      | drives      | drive      | 3rd person of drive                     |
      | broke       | break      | past of break                           |
      | broken      | break      | participle of break                     |
      | breaks      | break      | 3rd person of break                     |
      | chose       | choose     | past of choose (stemmer behavior)       |
      | chosen      | choose     | participle of choose (stemmer behavior) |
      | chooses     | choos      | 3rd person of choose (stemmer behavior) |
      | drank       | drink      | past of drink                           |
      | drunk       | drink      | participle of drink                     |
      | drinks      | drink      | 3rd person of drink                     |
      | flew        | fly        | past of fly (stemmer behavior)          |
      | flown       | fly        | participle of fly (stemmer behavior)    |
      | flies       | fli        | 3rd person of fly (stemmer behavior)    |
      | swam        | swim       | past of swim                            |
      | swum        | swim       | participle of swim                      |
      | swims       | swim       | 3rd person of swim                      |
      | rang        | ring       | past of ring                            |
      | rung        | ring       | participle of ring                      |
      | rings       | ring       | 3rd person of ring                      |
      | sang        | sing       | past of sing                            |
      | sung        | sing       | participle of sing                      |
      | sings       | sing       | 3rd person of sing                      |
      | sank        | sink       | past of sink                            |
      | sunk        | sink       | participle of sink                      |
      | sinks       | sink       | 3rd person of sink                      |
      | shook       | shake      | past of shake                           |
      | shaken      | shake      | participle of shake                     |
      | shakes      | shake      | 3rd person of shake                     |
      | stole       | steal      | past of steal                           |
      | stolen      | steal      | participle of steal                     |
      | steals      | steal      | 3rd person of steal                     |
      | swore       | swear      | past of swear                           |
      | sworn       | swear      | participle of swear                     |
      | swears      | swear      | 3rd person of swear                     |
      | threw       | throw      | past of throw                           |
      | thrown      | throw      | participle of throw                     |
      | throws      | throw      | 3rd person of throw                     |
      | wore        | wear       | past of wear                            |
      | worn        | wear       | participle of wear                      |
      | wears       | wear       | 3rd person of wear                      |
      | bit         | bite       | past of bite                            |
      | bitten      | bite       | participle of bite                      |
      | bites       | bite       | 3rd person of bite                      |
      | hid         | hide       | past of hide                            |
      | hidden      | hide       | participle of hide                      |
      | hides       | hide       | 3rd person of hide                      |
      | froze       | freeze     | past of freeze (stemmer behavior)       |
      | frozen      | freeze     | participle of freeze (stemmer behavior) |
      | freezes     | freez      | 3rd person of freeze (stemmer behavior) |
      | rose        | rise       | past of rise                            |
      | risen       | rise       | participle of rise                      |
      | rises       | rise       | 3rd person of rise                      |
      | woke        | wake       | past of wake                            |
      | woken       | wake       | participle of wake                      |
      | wakes       | wake       | 3rd person of wake                      |
      | wove        | weave      | past of weave (stemmer behavior)        |
      | woven       | weave      | participle of weave (stemmer behavior)  |
      | weaves      | weav       | 3rd person of weave (stemmer behavior)  |
      | tore        | tear       | past of tear                            |
      | torn        | tear       | participle of tear                      |
      | tears       | tear       | 3rd person of tear                      |
      | shrank      | shrink     | past of shrink                          |
      | shrunk      | shrink     | participle of shrink                    |
      | shrinks     | shrink     | 3rd person of shrink                    |
      | struck      | strike     | past of strike                          |
      | strikes     | strike     | 3rd person of strike                    |
      | sought      | seek       | past of seek                            |
      | seeks       | seek       | 3rd person of seek                      |
      | fought      | fight      | past of fight                           |
      | fights      | fight      | 3rd person of fight                     |
      | bound       | bind       | past of bind                            |
      | binds       | bind       | 3rd person of bind                      |
      | ground      | grind      | past of grind                           |
      | grinds      | grind      | 3rd person of grind                     |
      | wound       | wind       | past of wind                            |
      | winds       | wind       | 3rd person of wind                      |
      | spun        | spin       | past of spin                            |
      | spins       | spin       | 3rd person of spin                      |
      | clung       | cling      | past of cling                           |
      | clings      | cling      | 3rd person of cling                     |
      | stung       | sting      | past of sting                           |
      | stings      | sting      | 3rd person of sting                     |
      | swung       | swing      | past of swing                           |
      | swings      | swing      | 3rd person of swing                     |
      | wrung       | wring      | past of wring                           |
      | wrings      | wring      | 3rd person of wring                     |
      | slung       | sling      | past of sling                           |
      | slings      | sling      | 3rd person of sling                     |
      | stuck       | stick      | past of stick                           |
      | sticks      | stick      | 3rd person of stick                     |
      | dealt       | deal       | past of deal                            |
      | deals       | deal       | 3rd person of deal                      |
      | knelt       | kneel      | past of kneel                           |
      | kneels      | kneel      | 3rd person of kneel                     |
      | leant       | lean       | past of lean                            |
      | leans       | lean       | 3rd person of lean                      |
      | leapt       | leap       | past of leap                            |
      | leaps       | leap       | 3rd person of leap                      |
      | crept       | creep      | past of creep                           |
      | creeps      | creep      | 3rd person of creep                     |
      | wept        | weep       | past of weep                            |
      | weeps       | weep       | 3rd person of weep                      |
      | slept       | sleep      | past of sleep                           |
      | sleeps      | sleep      | 3rd person of sleep                     |
      | swept       | sweep      | past of sweep                           |
      | sweeps      | sweep      | 3rd person of sweep                     |
      | fed         | feed       | past of feed                            |
      | feeds       | feed       | 3rd person of feed                      |
      | bred        | breed      | past of breed                           |
      | breeds      | breed      | 3rd person of breed                     |
      | bled        | bleed      | past of bleed                           |
      | bleeds      | bleed      | 3rd person of bleed                     |
      | fled        | flee       | past of flee                            |
      | flees       | flee       | 3rd person of flee                      |
      | sped        | speed      | past of speed                           |
      | speeds      | speed      | 3rd person of speed                     |
      | shed        | shed       | past of shed                            |
      | sheds       | shed       | 3rd person of shed                      |
      | spread      | spread     | past of spread                          |
      | spreads     | spread     | 3rd person of spread                    |
      | bet         | bet        | past of bet                             |
      | bets        | bet        | 3rd person of bet                       |
      | cast        | cast       | past of cast                            |
      | casts       | cast       | 3rd person of cast                      |
      | cost        | cost       | past of cost                            |
      | costs       | cost       | 3rd person of cost                      |
      | shut        | shut       | past of shut                            |
      | shuts       | shut       | 3rd person of shut                      |
      | split       | split      | past of split                           |
      | splits      | split      | 3rd person of split                     |
      | let         | let        | past of let                             |
      | lets        | let        | 3rd person of let                       |
      | burst       | burst      | past of burst                           |
      | bursts      | burst      | 3rd person of burst                     |
      | hung        | hang       | past of hang                            |
      | hangs       | hang       | 3rd person of hang                      |
      | spat        | spit       | past of spit                            |
      | spits       | spit       | 3rd person of spit                      |
      | lit         | light      | past of light                           |
      | lights      | light      | 3rd person of light                     |
      | bid         | bid        | past of bid                             |
      | bids        | bid        | 3rd person of bid                       |

  Scenario Outline: Lemmatize irregular nouns (plural to singular)
    Given input is "<input>"
    When word is lemmatized
    Then output should be "<lemma>"
    Examples:
      | input       | lemma      | comments                            |
      | men         | man        | irregular plural                    |
      | women       | woman      | irregular plural                    |
      | children    | child      | irregular plural                    |
      | oxen        | ox         | irregular plural                    |
      | feet        | foot       | irregular plural                    |
      | geese       | gees       | irregular plural (stemmer behavior) |
      | teeth       | tooth      | irregular plural                    |
      | mice        | mouse      | irregular plural                    |
      | lice        | louse      | irregular plural                    |
      | brethren    | brother    | irregular plural                    |
      | analyses    | analys     | Greek plural (stemmer behavior)     |
      | bases       | base       | Greek plural                        |
      | crises      | crise      | Greek plural (stemmer behavior)     |
      | diagnoses   | diagnos    | Greek plural (stemmer behavior)     |
      | hypotheses  | hypothes   | Greek plural (stemmer behavior)     |
      | oases       | oas        | Greek plural (stemmer behavior)     |
      | parentheses | parenthes  | Greek plural (stemmer behavior)     |
      | theses      | these      | Greek plural (stemmer behavior)     |
      | axes        | axe        | Greek plural (stemmer behavior)     |
      | phenomena   | phenomenon | Greek plural                        |
      | criteria    | criterion  | Greek plural                        |
      | data        | datum      | Latin plural                        |
      | media       | medium     | Latin plural                        |
      | bacteria    | bacterium  | Latin plural                        |
      | curricula   | curriculum | Latin plural                        |
      | memoranda   | memorandum | Latin plural                        |
      | strata      | stratum    | Latin plural                        |
      | alumni      | alumnus    | Latin plural                        |
      | cacti       | cactus     | Latin plural                        |
      | foci        | focus      | Latin plural                        |
      | fungi       | fungus     | Latin plural                        |
      | nuclei      | nucleus    | Latin plural                        |
      | radii       | radius     | Latin plural                        |
      | stimuli     | stimulus   | Latin plural                        |
      | syllabi     | syllabus   | Latin plural                        |
      | appendices  | appendic   | Latin plural (stemmer behavior)     |
      | indices     | indic      | Latin plural (stemmer behavior)     |
      | matrices    | matric     | Latin plural (stemmer behavior)     |
      | vertices    | vertic     | Latin plural (stemmer behavior)     |
      | bureaux     | bureau     | French plural                       |
      | plateaux    | plateau    | French plural                       |
      | tableaux    | tableau    | French plural                       |
      | sheep       | sheep      | zero plural                         |
      | deer        | deer       | zero plural                         |
      | fish        | fish       | zero plural                         |
      | species     | speci      | zero plural (stemmer behavior)      |
      | series      | seri       | zero plural (stemmer behavior)      |
      | aircraft    | aircraft   | zero plural                         |
      | moose       | moos       | zero plural (stemmer behavior)      |
      | swine       | swine      | zero plural                         |

  Scenario Outline: Lemmatize irregular adjectives (comparative/superlative to base)
    Given input is "<input>"
    When word is lemmatized
    Then output should be "<lemma>"
    Examples:
      | input    | lemma  | comments                                 |
      | better   | good   | comparative of good                      |
      | best     | good   | superlative of good                      |
      | worse    | wors   | comparative of bad (stemmer behavior)    |
      | worst    | bad    | superlative of bad (found in IRREGULARS) |
      | less     | little | comparative of little                    |
      | least    | little | superlative of little                    |
      | further  | far    | comparative of far                       |
      | farthest | far    | superlative of far                       |
      | furthest | far    | superlative of far                       |
      | elder    | old    | comparative of old                       |
      | eldest   | old    | superlative of old                       |
      | more     | much   | comparative of much                      |
      | most     | much   | superlative of much                      |

  Scenario Outline: Lemmatize regular words
    Given input is "<input>"
    When word is lemmatized
    Then output should be "<lemma>"
    Examples:
      | input    | lemma    | comments                                 |
      | cats     | cat      | regular plural                           |
      | dogs     | dog      | regular plural                           |
      | boxes    | box      | regular plural                           |
      | watches  | watch    | regular plural                           |
      | flies    | fli      | regular plural (stemmer behavior)        |
      | running  | run      | present participle                       |
      | walked   | walk     | past tense                               |
      | jumping  | jump     | present participle                       |
      | played   | play     | past tense                               |
      | happier  | happier  | comparative adjective (stemmer behavior) |
      | happiest | happiest | superlative adjective (stemmer behavior) |
      | bigger   | bigger   | comparative adjective (stemmer behavior) |
      | biggest  | biggest  | superlative adjective (stemmer behavior) |
      | quickly  | quick    | adverb (stemmer behavior)                |
      | slowly   | slowli   | adverb                                   |
