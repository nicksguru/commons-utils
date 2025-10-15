package guru.nicks.exception;

import guru.nicks.exception.http.NotFoundException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.StandardException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Subclasses which are REST API exceptions (rendered in REST JSON responses) must be mentioned in an enum binding them
 * to error codes. Technically, it's sufficient to mention their parent classes only, in which case the error code and
 * the HTTP status are inherited from the parent. But usually a separate error code is needed in order to distinguish
 * one business case from another.
 * <p>
 * By default, the HTTP status code is set to 500 unless the subclass inherits from a status-aware exception, such as
 * {@link NotFoundException} and others in the same package.
 */
@StandardException(access = AccessLevel.PROTECTED)
public abstract class BusinessException extends RuntimeException {

    /**
     * Immutable map of additional headers to set in HTTP response (if their names and values are not blank; for values,
     * {@link Object#toString()} is called).
     * <p>
     * Map keys (header names) are converted to lowercase for consistency. It's preferable to stick to standard
     * {@link HttpHeaders}, prefixing non-standard ones with {@code X-}.
     */
    @Getter
    private Map<String, Object> additionalResponseHeaders;

    protected BusinessException(Map<String, Object> additionalResponseHeaders) {
        super();

        // convert header names to lowercase for consistency, for example in case caller reads them by known names
        if (additionalResponseHeaders != null) {
            this.additionalResponseHeaders = additionalResponseHeaders.entrySet()
                    .stream()
                    .filter(entry -> StringUtils.isNotBlank(entry.getKey()))
                    .filter(entry -> StringUtils.isNotBlank(Objects.toString(entry.getValue(), null)))
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().toLowerCase(),
                            entry -> entry.getValue().toString()));
        }
    }

}
