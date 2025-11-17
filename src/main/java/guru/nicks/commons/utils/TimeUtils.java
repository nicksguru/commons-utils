package guru.nicks.commons.utils;

import am.ik.yavi.meta.ConstraintArguments;
import jakarta.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Time-related utility methods.
 * <p>
 * NOTE: {@link #setCustomEpoch(Instant)} must be called before {@link #getDurationSinceCustomEpoch(Instant)},
 * otherwise the latter fails with an exception.
 */
@UtilityClass
@Slf4j
public class TimeUtils {

    /**
     * @see #convertHmsDurationToSeconds(String)
     */
    private static final Pattern SPLIT_DURATION_PATTERN = Pattern.compile("\\p{javaSpaceChar}*:\\p{javaSpaceChar}*");

    /**
     * {@code $1} spans fractional digits to retain
     *
     * @see #humanFormatDuration(Duration)
     */
    private static Instant customEpoch;

    /**
     * Getter for the custom Epoch.
     *
     * @return custom Epoch
     * @throws NullPointerException custom Epoch not set
     * @see #setCustomEpoch(Instant)
     */
    @SneakyThrows
    public static Instant getCustomEpoch() {
        return checkNotNull(customEpoch, "custom Epoch");
    }

    /**
     * Setter for the custom Epoch.
     *
     * @param customEpoch custom Epoch
     * @throws IllegalArgumentException {@code customEpoch} is {@code null}
     */
    @ConstraintArguments
    public static void setCustomEpoch(Instant customEpoch) {
        checkNotNull(customEpoch, _TimeUtilsSetCustomEpochArgumentsMeta.CUSTOMEPOCH.name());
        TimeUtils.customEpoch = customEpoch;
    }

    /**
     * Formats duration in a human-readable way, such as '5 days 4 minutes 3 seconds'. The largest unit is days.
     * Subseconds ('0.1 seconds') are only shown if the whole duration is shorter than a second and no shorter than a
     * millisecond.
     *
     * @param duration duration to format
     * @return duration formatted (empty string if it was {@code null})
     */
    public static String humanFormatDuration(@Nullable Duration duration) {
        if (duration == null) {
            return "";
        }

        String result = DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true);

        // replace '0 seconds' with '0.abc seconds' using millis (DurationFormatUtils doesn't output millis, therefore
        // subsecond durations are represented as '0 seconds' which is confusing)
        if ((duration.toSeconds() == 0) && (duration.getNano() > 0)) {
            long millis = Math.round(duration.getNano() / 1_000_000.0);

            // create without trailing zeroes: 0.1, or 0.12, or 0.123
            if (millis > 0) {
                result = String.format("%.3f", millis / 1000.0);
                result = Strings.CS.removeEnd(result, "0");
                result = Strings.CS.removeEnd(result, "0");
                result += " seconds";
            }
        }

        return result;
    }

    /**
     * Converts {@code h:m:s} duration to seconds. All the three components must always be present, for example:
     * {@code 0:12:34}, {@code 0:0:12}.
     *
     * @param hms duration
     * @return seconds (0 {@code null} if the argument was {@code null} or blank)
     * @throws IllegalArgumentException invalid duration format (must always have all the 3 digits)
     */
    public static int convertHmsDurationToSeconds(@Nullable String hms) {
        // length unknown
        if (StringUtils.isBlank(hms)) {
            return 0;
        }

        int hours;
        int minutes;
        int seconds;
        // h:m:s -> [h, m, s]
        String[] parts = SPLIT_DURATION_PATTERN.split(hms);

        try {
            if (parts.length != 3) {
                throw new IllegalArgumentException();
            }

            hours = Integer.parseUnsignedInt(parts[0]);
            minutes = Integer.parseUnsignedInt(parts[1]);
            seconds = Integer.parseUnsignedInt(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Duration must be H:M:S (even if H=0 or M=0)");
        }

        return hours * 3600 + minutes * 60 + seconds;
    }

    /**
     * Calculates duration between the given timestamp and custom Epoch (or, if custom Epoch is not set, since Unix
     * epoch). Nanoseconds are preserved.
     *
     * @param timestamp timestamp
     * @return duration
     * @throws IllegalArgumentException {@code timestamp} is earlier than the custom Epoch or the custom Epoch not set
     */
    @ConstraintArguments
    public Duration getDurationSinceCustomEpoch(Instant timestamp) {
        if (customEpoch != null) {
            check(timestamp, _TimeUtilsGetDurationSinceCustomEpochArgumentsMeta.TIMESTAMP.name())
                    .notNull()
                    .constraint(instant -> !instant.isBefore(customEpoch), "must belong to custom Epoch");
        }

        long seconds = timestamp.getEpochSecond();
        if (customEpoch != null) {
            seconds -= customEpoch.getEpochSecond();
        }

        int nanos = timestamp.getNano();
        if (customEpoch != null) {
            seconds -= customEpoch.getNano();
        }

        return Duration.ofSeconds(seconds, nanos);
    }

    /**
     * Converts milliseconds to seconds with default precision (2).
     *
     * @param millis milliseconds
     * @return seconds
     */
    public BigDecimal convertMillisToSeconds(long millis) {
        return convertMillisToSeconds(millis, 3);
    }

    /**
     * Converts milliseconds to seconds with the given precision.
     *
     * @param millis                 milliseconds
     * @param numberOfFractionDigits precision
     * @return seconds
     */
    public BigDecimal convertMillisToSeconds(long millis, int numberOfFractionDigits) {
        return round(millis / 1000.0, numberOfFractionDigits);
    }

    /**
     * Rounds number to the given precision.
     *
     * @param number                 number to round
     * @param numberOfFractionDigits precision
     * @return number rounded
     */
    public BigDecimal round(double number, int numberOfFractionDigits) {
        return BigDecimal
                .valueOf(number)
                .setScale(numberOfFractionDigits, RoundingMode.HALF_UP);
    }

}
