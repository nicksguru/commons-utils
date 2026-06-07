package guru.nicks.commons.utils.text;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Text-related utility methods.
 */
@UtilityClass
public class TextUtils {

    public static final Predicate<String> ALL_ZEROES_PREDICATE = Pattern.compile("^0+$").asMatchPredicate();

    /**
     * WARNING: {@code javaSpaceChar} does not span '\r\n\t\v' ({@code javaWhitespace} does). But the latter does not
     * span Unicode spaces, therefore these two should be combined.
     */
    private static final String ANY_WHITESPACE = "\\p{javaSpaceChar}\\p{javaWhitespace}";

    /**
     * Pre-compiled (to avoid repetitive on the fly recompilation) regexp that matches '\r\n\t\v',
     * {@code "!#$%&'()*+,-./:;<=>?@[\]^_`{ |}~}, and Unicode whitespaces.
     */
    private static final Pattern SPLIT_INTO_WORDS_PATTERN = Pattern.compile("[\\p{Punct}" + ANY_WHITESPACE + "]+");

    /**
     * Pre-compiled (to avoid repetitive on the fly recompilation) regexp that matches a comma surrounded by one or more
     * {@link Character#isWhitespace(char)}. The latter matches Unicode whitespaces - much more than {@code \s} which is
     * for ASCII only.
     */
    private static final Pattern SPLIT_BY_COMMA_PATTERN = Pattern.compile(
            "[" + ANY_WHITESPACE + "]*,[" + ANY_WHITESPACE + "]*");
    private static final Pattern SPLIT_BY_WHITESPACES_PATTERN = Pattern.compile(
            "[" + ANY_WHITESPACE + "]+");

    /**
     * Each range is split into 3 parts:
     * <ul>
     *     <li>{@link Range#singleton(Comparable)} - range start (e.g. X=10)</li>
     *     <li>{@link Range#open(Comparable, Comparable)} - less than 2 times X ('more than X', e.g. 'more than 10'
     *         means 11, 12, ..., 19)</li>
     *     <li>{@link Range#closedOpen(Comparable, Comparable)} - 2 times X and up to the next range ('Xs of', e.g.
     *         'tens of' means 20, 21, ..., 99)</li>
     * </ul>
     */
    private static final RangeMap<Long, String> MAGNITUDE_RANGES;

    static {
        MAGNITUDE_RANGES = ImmutableRangeMap.<Long, String>builder()
                // < 10
                .put(Range.lessThan(0L), "negative")
                .put(Range.singleton(0L), "zero")
                .put(Range.open(0L, 10L), "several")
                // < 100
                .put(Range.singleton(10L), "ten")
                .put(Range.open(10L, 20L), "more than ten")
                .put(Range.closedOpen(20L, 100L), "tens of")
                // < 1000
                .put(Range.singleton(100L), "a hundred")
                .put(Range.open(100L, 200L), "more than a hundred")
                .put(Range.closedOpen(200L, 1000L), "hundreds of")
                // < 10 000
                .put(Range.singleton(1000L), "a thousand")
                .put(Range.open(1000L, 2000L), "more than a thousand")
                .put(Range.closedOpen(2000L, 10_000L), "thousands of")
                // < 100 000
                .put(Range.singleton(10_000L), "ten thousand")
                .put(Range.open(10_000L, 11_000L), "more than ten thousand")
                .put(Range.closedOpen(11_000L, 100_000L), "tens of thousands")
                // < 1 000 000
                .put(Range.singleton(100_000L), "a hundred thousand")
                .put(Range.open(100_000L, 200_000L), "more than a hundred thousand")
                .put(Range.closedOpen(200_000L, 1_000_000L), "hundreds of thousands")
                // < 10 000 000
                .put(Range.singleton(1_000_000L), "a million")
                .put(Range.open(1_000_000L, 2_000_000L), "more than a million")
                .put(Range.closedOpen(2_000_000L, 10_000_000L), "millions of")
                // < 100 000 000
                .put(Range.singleton(10_000_000L), "ten million")
                .put(Range.open(10_000_000L, 20_000_000L), "more than ten million")
                .put(Range.closedOpen(20_000_000L, 100_000_000L), "tens of millions")
                // < 1 000 000 000
                .put(Range.singleton(100_000_000L), "a hundred million")
                .put(Range.open(100_000_000L, 200_000_000L), "more than a hundred million")
                .put(Range.closedOpen(200_000_000L, 1_000_000_000L), "hundreds of millions")
                // max
                .put(Range.singleton(1_000_000_000L), "a billion")
                .put(Range.open(1_000_000_000L, 2_000_000_000L), "more than a billion")
                .put(Range.atLeast(2_000_000_000L), "billions of")
                .build();
    }

    /**
     * Extracts unique words from string.
     *
     * @param str           input string
     * @param reduceAccents if {@code true}, accented characters are reduced to their base ones, such as {@code ä → a}
     * @return words (sorted alphabetically), in lowercase, modifiable collection
     */
    public static SortedSet<String> collectUniqueWords(@Nullable String str, boolean reduceAccents) {
        if (StringUtils.isBlank(str)) {
            // has to be modifiable, see contract
            return new TreeSet<>();
        }

        if (reduceAccents) {
            str = reduceAccents(str);
        }

        return new TreeSet<>(splitIntoWords(str.toLowerCase()));
    }

    /**
     * Splits string into (non-unique) words by whitespace and punctuation characters.
     *
     * @param str input string
     * @return words (with character case preserved), modifiable collection
     * @see #collectUniqueWords(String, boolean)
     */
    public static List<String> splitIntoWords(@Nullable String str) {
        if (StringUtils.isBlank(str)) {
            // has to be modifiable, see contract
            return new ArrayList<>(0);
        }

        String[] array = SPLIT_INTO_WORDS_PATTERN.split(str);
        return toListWithoutBlankStrings(array);
    }

    /**
     * Splits string into (non-unique) items by a comma surrounded by optional whitespaces. To retain unique items only,
     * pass the result to an appropriate {@link Set} implementation.
     *
     * @param str input string
     * @return items (with character case preserved), modifiable collection
     */
    public static List<String> splitByComma(@Nullable String str) {
        if (StringUtils.isBlank(str)) {
            // has to be modifiable
            return new ArrayList<>(0);
        }

        String[] array = SPLIT_BY_COMMA_PATTERN.split(str);
        return toListWithoutBlankStrings(array);
    }

    /**
     * Splits string into (non-unique) items by whitespaces. To retain unique items only, pass the result to a
     * {@link Set}.
     *
     * @param str input string
     * @return items (with character case preserved), modifiable collection
     */
    public static List<String> splitByWhitespaces(@Nullable String str) {
        if (StringUtils.isBlank(str)) {
            // has to be modifiable
            return new ArrayList<>(0);
        }

        String[] array = SPLIT_BY_WHITESPACES_PATTERN.split(str);
        return toListWithoutBlankStrings(array);
    }

    /**
     * Removes punctuation from string. Actually splits the string into words and then joins them with a single space.
     *
     * @param str input string
     * @return converted string ({@code null} if input string is {@code null})
     * @see #splitIntoWords(String)
     */
    @Nullable
    public static String removePunctuation(@Nullable String str) {
        return (str == null)
                ? null
                : String.join(" ", splitIntoWords(str));
    }

    /**
     * Reduces all accented characters to corresponding non-accented ones according to
     * <a href="https://www.unicode.org/charts/normalization">Unicode Normalization Charts</a>, for example {@code
     * ä -> a}. Also, replaces: {@code ë -> e; Ё -> ё} (special case for Russian).
     *
     * @param str input string
     * @return converted string ({@code null} if the input string is {@code null})
     */
    @Nullable
    public static String reduceAccents(@Nullable String str) {
        if (str == null) {
            return null;
        }

        String result = StringUtils.stripAccents(str);
        // special replacement for Russian 'ё'/'Ё'
        result = result.replace('ё', 'е').replace('Ё', 'Е');
        return result;
    }

    /**
     * Returns a qualitative representation of the magnitude of the count, such as 'tens of'.
     *
     * @return magnitude of {@code count} in a human-readable form, or '&lt;???&gt;' if unknown (which is an error, but
     *         there's no reason to crash the application because of that)
     */
    public static String getMagnitudeOfCount(long count) {
        String result = MAGNITUDE_RANGES.get(count);

        return (result == null)
                ? "<???>"
                : result;
    }

    /**
     * Converts string array to list without empty strings.
     *
     * @param array string array
     * @return modifiable list without blank strings
     */
    private static ArrayList<String> toListWithoutBlankStrings(String[] array) {
        var result = new ArrayList<String>(array.length);

        for (String str : array) {
            if (!StringUtils.isBlank(str)) {
                result.add(str);
            }
        }

        return result;
    }

}
