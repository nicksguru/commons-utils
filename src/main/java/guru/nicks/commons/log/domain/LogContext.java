package guru.nicks.commons.log.domain;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Defines how each enum member is inserted in log messages (via {@code %X} or {@code %X{mdcName}}).
 */
@RequiredArgsConstructor
@Getter
public enum LogContext {

    APP_NAME("app.name"),
    REMOTE_IP("remote.ip"),

    /**
     * Current user.
     */
    USERNAME("user.name"),

    /**
     * Generated and stored in {@link MDC} by Micrometer implicitly, the name is always {@code traceId}, but it's
     * rendered in logs as {@code trace_id}. Acts as an internally assigned HTTP request ID. Propagated to remote calls
     * automatically via {@code RestTemplate} and {@code Feign} instrumentation.
     * <p>
     * For more details, see <a
     * href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/logger-mdc-instrumentation.md">
     * Uptrace documentation</a>.
     */
    TRACE_ID("traceId"),

    /**
     * REST
     */
    REQUEST_PATH("request.path"),
    REQUEST_METHOD("request.method"),
    RESPONSE_HTTP_STATUS("response.http_status"),
    RESPONSE_MS_ELAPSED("response.ms_elapsed"),

    /**
     * JVM statistics.
     */
    RAM_FREE_MB("ram.free_mb"),
    RAM_MAX_MB("ram.max_mb"),

    MESSAGE_ID("message.id"),
    MESSAGE_TOPIC("message.topic"),

    /**
     * Cron job being executed.
     */
    JOB_NAME("job.name"),

    EVENT_TYPE("event.type"),
    EVENT_DATA("event.data");

    /**
     * Response header where current request ID (a.k.a. trace ID) is returned.
     */
    public static final String RESPONSE_TRACE_ID_HEADER = "x-trace-id";

    /**
     * Response header where the number of milliseconds request processing took is returned.
     */
    public static final String RESPONSE_MS_ELAPSED_HEADER = "x-ms-elapsed";

    /**
     * Parameter name in {@link MDC}.
     */
    private final String nameInMdc;

    /**
     * Clears all {@link LogContext#values()}, so there's no need to call {@link LogContext#clear()} for each value.
     */
    public static void clearAll() {
        for (var value : values()) {
            value.clear();
        }
    }

    /**
     * Removes argument from {@link MDC}.
     */
    public void clear() {
        MDC.remove(nameInMdc);
    }

    /**
     * Retrieves value out of {@link MDC}.
     *
     * @return non-blank value (blank value becomes {@link Optional#empty()})
     */
    public Optional<String> find() {
        return Optional
                .ofNullable(MDC.get(nameInMdc))
                .filter(StringUtils::isNotBlank);
    }

    /**
     * Calls {@link #find()} and throws exception on null/blank value.
     *
     * @return value (non-blank string)
     * @throws NoSuchElementException value missing or blank
     */
    public String get() {
        return find().orElseThrow(() -> new NoSuchElementException(nameInMdc));
    }

    /**
     * Checks if the value is non-blank.
     *
     * @return {@code true} if {@link #find()} returns a non-empty {@link Optional}.
     */
    public boolean exists() {
        return find().isPresent();
    }

    /**
     * Saves argument in {@link MDC} in its stringified form (as per {@link Object#toString}). If the resulting string
     * is blank, removes the key from {@link MDC}.
     *
     * @param value value to put (or remove)
     */
    public void put(@Nullable Object value) {
        Optional.ofNullable(value)
                .map(Object::toString)
                .filter(StringUtils::isNotBlank)
                .ifPresentOrElse(
                        it -> MDC.put(nameInMdc, it),
                        this::clear);
    }

}
