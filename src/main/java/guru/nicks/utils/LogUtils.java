package guru.nicks.utils;

import guru.nicks.log.domain.LogContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Logging-related utility methods.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LogUtils {

    // DI
    private final ObjectMapper objectMapper;

    /**
     * Logs message augmented with event type and data in JSON format.
     *
     * @param eventType event type ({@link LogContext#EVENT_TYPE})
     * @param eventData optional event data ({@link LogContext#EVENT_DATA})
     * @param message   log message
     */
    public void logEvent(String eventType, @Nullable Object eventData, String message) {
        if (eventData != null) {
            // do this before adding other fields to LogContext, so JSON exception won't leave garbage there
            // (of add 'finally' to clear LogContext)
            try {
                LogContext.EVENT_DATA.put(objectMapper.writeValueAsString(eventData));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("JSON error: " + e.getMessage(), e);
            }
        }

        LogContext.EVENT_TYPE.put(eventType);

        try {
            log.info("[EVENT] {}", message);
        } finally {
            LogContext.EVENT_TYPE.clear();
            LogContext.EVENT_DATA.clear();
        }
    }

    /**
     * Logs message augmented with event type.
     *
     * @param eventType event type ({@link LogContext#EVENT_TYPE})
     * @param message   log message
     */
    public void logEvent(String eventType, String message) {
        logEvent(eventType, null, message);
    }

}
