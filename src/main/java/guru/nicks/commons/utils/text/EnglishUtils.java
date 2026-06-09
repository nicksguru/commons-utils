package guru.nicks.commons.utils.text;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import rita.RiTa;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * English morphology utility methods for lemmatization and stop word detection.
 */
@UtilityClass
public class EnglishUtils {

    /**
     * Common English stop words (NOT filtered out during lemmatization).
     */
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the",
            "and", "or", "but", "if", "while",
            "in", "on", "at", "to", "for", "of", "with", "by", "from", "as", "into",
            "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did",
            "this", "that", "these", "those",
            "i", "you", "he", "she", "it", "we", "they",
            "me", "him", "her", "us", "them",
            "my", "your", "his", "its", "our", "their");

    /**
     * Maps many (but not all) irregular English forms to their base lemmas, such as 'be' for 'was'. This covers the
     * gaps that standard algorithmic stemmers (like Postgres Snowball) miss.
     */
    private static final Map<String, String> IRREGULARS = HashMap.newHashMap(350);

    static {
        // ==========================================
        // IRREGULAR VERBS (Past / Participle / 3rd Person -> Infinitive)
        // ==========================================
        // Be
        IRREGULARS.put("was", "be");
        IRREGULARS.put("were", "be");
        IRREGULARS.put("am", "be");
        IRREGULARS.put("is", "be");
        IRREGULARS.put("are", "be");
        IRREGULARS.put("been", "be");
        // Have
        IRREGULARS.put("had", "have");
        IRREGULARS.put("has", "have");
        // Do
        IRREGULARS.put("did", "do");
        IRREGULARS.put("does", "do");
        IRREGULARS.put("done", "do");
        // Go
        IRREGULARS.put("went", "go");
        IRREGULARS.put("gone", "go");
        IRREGULARS.put("goes", "go");
        // Common irregulars
        IRREGULARS.put("ran", "run");
        IRREGULARS.put("runs", "run");
        IRREGULARS.put("ate", "eat");
        IRREGULARS.put("eaten", "eat");
        IRREGULARS.put("eats", "eat");
        IRREGULARS.put("saw", "see");
        IRREGULARS.put("seen", "see");
        IRREGULARS.put("sees", "see");
        IRREGULARS.put("came", "come");
        IRREGULARS.put("comes", "come");
        IRREGULARS.put("took", "take");
        IRREGULARS.put("taken", "take");
        IRREGULARS.put("takes", "take");
        IRREGULARS.put("made", "make");
        IRREGULARS.put("makes", "make");
        IRREGULARS.put("gave", "give");
        IRREGULARS.put("given", "give");
        IRREGULARS.put("gives", "give");
        IRREGULARS.put("knew", "know");
        IRREGULARS.put("known", "know");
        IRREGULARS.put("knows", "know");
        IRREGULARS.put("got", "get");
        IRREGULARS.put("gotten", "get");
        IRREGULARS.put("gets", "get");
        IRREGULARS.put("found", "find");
        IRREGULARS.put("finds", "find");
        IRREGULARS.put("thought", "think");
        IRREGULARS.put("thinks", "think");
        IRREGULARS.put("told", "tell");
        IRREGULARS.put("tells", "tell");
        IRREGULARS.put("became", "become");
        IRREGULARS.put("becomes", "become");
        IRREGULARS.put("left", "leave");
        IRREGULARS.put("leaves", "leave"); // "leaves" as noun handled below
        IRREGULARS.put("felt", "feel");
        IRREGULARS.put("feels", "feel");
        IRREGULARS.put("brought", "bring");
        IRREGULARS.put("brings", "bring");
        IRREGULARS.put("began", "begin");
        IRREGULARS.put("begun", "begin");
        IRREGULARS.put("begins", "begin");
        IRREGULARS.put("kept", "keep");
        IRREGULARS.put("keeps", "keep");
        IRREGULARS.put("held", "hold");
        IRREGULARS.put("holds", "hold");
        IRREGULARS.put("wrote", "write");
        IRREGULARS.put("written", "write");
        IRREGULARS.put("writes", "write");
        IRREGULARS.put("stood", "stand");
        IRREGULARS.put("stands", "stand");
        IRREGULARS.put("heard", "hear");
        IRREGULARS.put("hears", "hear");
        IRREGULARS.put("meant", "mean");
        IRREGULARS.put("means", "mean");
        IRREGULARS.put("set", "set");
        IRREGULARS.put("sets", "set");
        IRREGULARS.put("met", "meet");
        IRREGULARS.put("meets", "meet");
        IRREGULARS.put("paid", "pay");
        IRREGULARS.put("pays", "pay");
        IRREGULARS.put("sat", "sit");
        IRREGULARS.put("sits", "sit");
        IRREGULARS.put("spoke", "speak");
        IRREGULARS.put("spoken", "speak");
        IRREGULARS.put("speaks", "speak");
        IRREGULARS.put("led", "lead");
        IRREGULARS.put("leads", "lead"); // "lead" as noun handled below
        IRREGULARS.put("read", "read");
        IRREGULARS.put("reads", "read");
        IRREGULARS.put("grew", "grow");
        IRREGULARS.put("grown", "grow");
        IRREGULARS.put("grows", "grow");
        IRREGULARS.put("lost", "lose");
        IRREGULARS.put("loses", "lose");
        IRREGULARS.put("fell", "fall");
        IRREGULARS.put("fallen", "fall");
        IRREGULARS.put("falls", "fall");
        IRREGULARS.put("sent", "send");
        IRREGULARS.put("sends", "send");
        IRREGULARS.put("built", "build");
        IRREGULARS.put("builds", "build");
        IRREGULARS.put("understood", "understand");
        IRREGULARS.put("understands", "understand");
        IRREGULARS.put("cut", "cut");
        IRREGULARS.put("cuts", "cut");
        IRREGULARS.put("put", "put");
        IRREGULARS.put("puts", "put");
        IRREGULARS.put("hit", "hit");
        IRREGULARS.put("hits", "hit");
        IRREGULARS.put("bought", "buy");
        IRREGULARS.put("buys", "buy");
        IRREGULARS.put("caught", "catch");
        IRREGULARS.put("catches", "catch");
        IRREGULARS.put("drew", "draw");
        IRREGULARS.put("drawn", "draw");
        IRREGULARS.put("draws", "draw");
        IRREGULARS.put("drove", "drive");
        IRREGULARS.put("driven", "drive");
        IRREGULARS.put("drives", "drive");
        IRREGULARS.put("broke", "break");
        IRREGULARS.put("broken", "break");
        IRREGULARS.put("breaks", "break");
        IRREGULARS.put("chose", "choose");
        IRREGULARS.put("chosen", "choose");
        IRREGULARS.put("chooses", "choose");
        IRREGULARS.put("drank", "drink");
        IRREGULARS.put("drunk", "drink");
        IRREGULARS.put("drinks", "drink");
        IRREGULARS.put("flew", "fly");
        IRREGULARS.put("flown", "fly");
        IRREGULARS.put("flies", "fly");
        IRREGULARS.put("swam", "swim");
        IRREGULARS.put("swum", "swim");
        IRREGULARS.put("swims", "swim");
        IRREGULARS.put("rang", "ring");
        IRREGULARS.put("rung", "ring");
        IRREGULARS.put("rings", "ring");
        IRREGULARS.put("sang", "sing");
        IRREGULARS.put("sung", "sing");
        IRREGULARS.put("sings", "sing");
        IRREGULARS.put("sank", "sink");
        IRREGULARS.put("sunk", "sink");
        IRREGULARS.put("sinks", "sink");
        IRREGULARS.put("shook", "shake");
        IRREGULARS.put("shaken", "shake");
        IRREGULARS.put("shakes", "shake");
        IRREGULARS.put("stole", "steal");
        IRREGULARS.put("stolen", "steal");
        IRREGULARS.put("steals", "steal");
        IRREGULARS.put("swore", "swear");
        IRREGULARS.put("sworn", "swear");
        IRREGULARS.put("swears", "swear");
        IRREGULARS.put("threw", "throw");
        IRREGULARS.put("thrown", "throw");
        IRREGULARS.put("throws", "throw");
        IRREGULARS.put("wore", "wear");
        IRREGULARS.put("worn", "wear");
        IRREGULARS.put("wears", "wear");
        IRREGULARS.put("bit", "bite");
        IRREGULARS.put("bitten", "bite");
        IRREGULARS.put("bites", "bite");
        IRREGULARS.put("hid", "hide");
        IRREGULARS.put("hidden", "hide");
        IRREGULARS.put("hides", "hide");
        IRREGULARS.put("froze", "freeze");
        IRREGULARS.put("frozen", "freeze");
        IRREGULARS.put("freezes", "freeze");
        IRREGULARS.put("rose", "rise");
        IRREGULARS.put("risen", "rise");
        IRREGULARS.put("rises", "rise");
        IRREGULARS.put("woke", "wake");
        IRREGULARS.put("woken", "wake");
        IRREGULARS.put("wakes", "wake");
        IRREGULARS.put("wove", "weave");
        IRREGULARS.put("woven", "weave");
        IRREGULARS.put("weaves", "weave");
        IRREGULARS.put("tore", "tear");
        IRREGULARS.put("torn", "tear");
        IRREGULARS.put("tears", "tear");
        IRREGULARS.put("shrank", "shrink");
        IRREGULARS.put("shrunk", "shrink");
        IRREGULARS.put("shrinks", "shrink");
        IRREGULARS.put("struck", "strike");
        IRREGULARS.put("strikes", "strike");
        IRREGULARS.put("sought", "seek");
        IRREGULARS.put("seeks", "seek");
        IRREGULARS.put("fought", "fight");
        IRREGULARS.put("fights", "fight");
        IRREGULARS.put("bound", "bind");
        IRREGULARS.put("binds", "bind");
        IRREGULARS.put("ground", "grind");
        IRREGULARS.put("grinds", "grind");
        IRREGULARS.put("wound", "wind");
        IRREGULARS.put("winds", "wind");
        IRREGULARS.put("spun", "spin");
        IRREGULARS.put("spins", "spin");
        IRREGULARS.put("clung", "cling");
        IRREGULARS.put("clings", "cling");
        IRREGULARS.put("stung", "sting");
        IRREGULARS.put("stings", "sting");

        IRREGULARS.put("swung", "swing");
        IRREGULARS.put("swings", "swing");
        IRREGULARS.put("wrung", "wring");
        IRREGULARS.put("wrings", "wring");
        IRREGULARS.put("slung", "sling");
        IRREGULARS.put("slings", "sling");
        IRREGULARS.put("stuck", "stick");
        IRREGULARS.put("sticks", "stick");
        IRREGULARS.put("dealt", "deal");
        IRREGULARS.put("deals", "deal");
        IRREGULARS.put("knelt", "kneel");
        IRREGULARS.put("kneels", "kneel");
        IRREGULARS.put("leant", "lean");
        IRREGULARS.put("leans", "lean");
        IRREGULARS.put("leapt", "leap");
        IRREGULARS.put("leaps", "leap");
        IRREGULARS.put("crept", "creep");
        IRREGULARS.put("creeps", "creep");
        IRREGULARS.put("wept", "weep");
        IRREGULARS.put("weeps", "weep");
        IRREGULARS.put("slept", "sleep");
        IRREGULARS.put("sleeps", "sleep");
        IRREGULARS.put("swept", "sweep");
        IRREGULARS.put("sweeps", "sweep");
        IRREGULARS.put("fed", "feed");
        IRREGULARS.put("feeds", "feed");
        IRREGULARS.put("bred", "breed");
        IRREGULARS.put("breeds", "breed");
        IRREGULARS.put("bled", "bleed");
        IRREGULARS.put("bleeds", "bleed");
        IRREGULARS.put("fled", "flee");
        IRREGULARS.put("flees", "flee");
        IRREGULARS.put("sped", "speed");
        IRREGULARS.put("speeds", "speed");
        IRREGULARS.put("shed", "shed");
        IRREGULARS.put("sheds", "shed");
        IRREGULARS.put("spread", "spread");
        IRREGULARS.put("spreads", "spread");
        IRREGULARS.put("bet", "bet");
        IRREGULARS.put("bets", "bet");
        IRREGULARS.put("cast", "cast");
        IRREGULARS.put("casts", "cast");
        IRREGULARS.put("cost", "cost");
        IRREGULARS.put("costs", "cost");
        IRREGULARS.put("shut", "shut");
        IRREGULARS.put("shuts", "shut");
        IRREGULARS.put("split", "split");
        IRREGULARS.put("splits", "split");
        IRREGULARS.put("let", "let");
        IRREGULARS.put("lets", "let");
        IRREGULARS.put("burst", "burst");
        IRREGULARS.put("bursts", "burst");
        IRREGULARS.put("hung", "hang");
        IRREGULARS.put("hangs", "hang");
        IRREGULARS.put("spat", "spit");
        IRREGULARS.put("spits", "spit");
        IRREGULARS.put("lit", "light");
        IRREGULARS.put("lights", "light");
        IRREGULARS.put("bid", "bid");
        IRREGULARS.put("bids", "bid");

        // ==========================================
        // IRREGULAR NOUNS (Plural -> Singular)
        // ==========================================
        // Vowel changes / Old English
        IRREGULARS.put("men", "man");
        IRREGULARS.put("women", "woman");
        IRREGULARS.put("children", "child");
        IRREGULARS.put("oxen", "ox");
        IRREGULARS.put("feet", "foot");
        IRREGULARS.put("geese", "goose");
        IRREGULARS.put("teeth", "tooth");
        IRREGULARS.put("mice", "mouse");
        IRREGULARS.put("lice", "louse");
        IRREGULARS.put("brethren", "brother");

        // Latin/Greek origin plurals (Common in technical/academic search)
        IRREGULARS.put("analyses", "analysis");
        IRREGULARS.put("bases", "base"); // Also plural of basis, but base is a safer lemma for search
        IRREGULARS.put("crises", "crisis");
        IRREGULARS.put("diagnoses", "diagnosis");
        IRREGULARS.put("hypotheses", "hypothesis");
        IRREGULARS.put("oases", "oasis");
        IRREGULARS.put("parentheses", "parenthesis");
        IRREGULARS.put("theses", "thesis");
        IRREGULARS.put("axes", "axis");
        IRREGULARS.put("phenomena", "phenomenon");
        IRREGULARS.put("criteria", "criterion");
        IRREGULARS.put("data", "datum"); // Often treated as mass noun, but mathematically correct
        IRREGULARS.put("media", "medium");
        IRREGULARS.put("bacteria", "bacterium");
        IRREGULARS.put("curricula", "curriculum");
        IRREGULARS.put("memoranda", "memorandum");
        IRREGULARS.put("strata", "stratum");
        IRREGULARS.put("alumni", "alumnus");
        IRREGULARS.put("cacti", "cactus"); // cactuses also valid, but cacti common
        IRREGULARS.put("foci", "focus");
        IRREGULARS.put("fungi", "fungus");
        IRREGULARS.put("nuclei", "nucleus");
        IRREGULARS.put("radii", "radius");
        IRREGULARS.put("stimuli", "stimulus");
        IRREGULARS.put("syllabi", "syllabus");
        IRREGULARS.put("appendices", "appendix");
        IRREGULARS.put("indices", "index"); // indexes also valid
        IRREGULARS.put("matrices", "matrix");
        IRREGULARS.put("vertices", "vertex");
        IRREGULARS.put("bureaux", "bureau"); // bureaus also valid
        IRREGULARS.put("plateaux", "plateau");
        IRREGULARS.put("tableaux", "tableau");

        // Unchanging / Zero Plurals (Search indexes benefit from explicit mapping here)
        IRREGULARS.put("sheep", "sheep");
        IRREGULARS.put("deer", "deer");
        IRREGULARS.put("fish", "fish"); // fishes exists for species, but fish is primary
        IRREGULARS.put("species", "species");
        IRREGULARS.put("series", "series");
        IRREGULARS.put("aircraft", "aircraft");
        IRREGULARS.put("moose", "moose");
        IRREGULARS.put("swine", "swine");

        // ==========================================
        // IRREGULAR ADJECTIVES (Comparative/Superlative -> Base)
        // ==========================================
        IRREGULARS.put("better", "good");
        IRREGULARS.put("best", "good");
        IRREGULARS.put("worse", "bad");
        IRREGULARS.put("worst", "bad");
        IRREGULARS.put("less", "little");
        IRREGULARS.put("least", "little");
        IRREGULARS.put("further", "far");
        IRREGULARS.put("farthest", "far");
        IRREGULARS.put("furthest", "far");
        IRREGULARS.put("elder", "old");
        IRREGULARS.put("eldest", "old");
        IRREGULARS.put("more", "much");
        IRREGULARS.put("most", "much");
    }

    /**
     * Converts the English word to its base form, taking into account many irregular words. Common
     * {@link #stopWord(String) stop words} are not filtered out, rather processed, e.g. 'was' → 'be'.
     *
     * @param word (will be converted to lowercase, and punctuation removed)
     * @return lemma, or the original word if it wasn't recognized as an English word (e.g. has punctuation characters
     *         or belongs to another language)
     */
    public static String getWordLemma(String word) {
        if (StringUtils.isBlank(word)) {
            return word;
        }

        word = word.strip().toLowerCase();
        String stem = RiTa.stem(word);
        String regular = IRREGULARS.get(stem);

        return StringUtils.isNotBlank(regular)
                ? regular
                : stem;
    }

    /**
     * Checks for common stop words, such as 'the', 'a', 'it'.
     *
     * @param word will be converted to lowercase, and leading/trailing whitespaces removed
     * @return {@code true} if the word is a stop word
     */
    public static boolean stopWord(String word) {
        if (StringUtils.isBlank(word)) {
            return false;
        }

        word = word.strip().toLowerCase();
        return STOP_WORDS.contains(word);
    }

}
