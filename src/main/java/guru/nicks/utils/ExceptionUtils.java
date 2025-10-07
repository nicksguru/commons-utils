package guru.nicks.utils;

import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Formats stack traces.
 */
@UtilityClass
public class ExceptionUtils {

    public static final Set<String> OMITTED_CLASS_PREFIXES = Set.of(
            "brave.servlet.",
            "java.lang.invoke.",
            "java.net.AbstractPlainSocketImpl",
            "jakarta.servlet.",
            "javax.servlet.",
            "jdk.internal.",

            "org.springframework.cglib.",
            "org.springframework.security.web.access.ExceptionTranslationFilter",
            "org.springframework.integration.",
            "org.springframework.messaging.",
            "org.springframework.aop.framework.",
            "org.springframework.security.web.context",
            "org.springframework.security.web.header",
            "org.springframework.security.web.FilterChainProxy",
            "org.springframework.security.web.ObservationFilterChainDecorator",
            "org.springframework.security.web.session",
            "org.springframework.security.web.servletapi.",
            "org.springframework.boot.actuate.metrics.",

            "reactor.core.",
            "okhttp3.internal",
            "io.undertow.",

            "org.jboss.threads.",
            "org.apache.catalina.",
            "org.apache.coyote.",
            "org.apache.tomcat.");

    /**
     * Formats exception message, adding its stack trace with trivial frames (such as servlets) omitted.
     *
     * @param t exception
     * @return message suitable for logging (empty string if the exception is {@code null})
     */
    public static String formatWithCompactStackTrace(@Nullable Throwable t) {
        if (t == null) {
            return "";
        }

        var messageBuilder = new StringBuilder(500)
                .append(t.getClass().getName());

        // log exception message
        if (StringUtils.isNotBlank(t.getMessage())) {
            messageBuilder
                    .append("('")
                    .append(t.getMessage())
                    .append("')");
        }

        Throwable rootCause = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(t);

        // log exception cause if it's not the same as the original exception
        if ((rootCause != null) && (rootCause != t)) {
            messageBuilder
                    .append(" with root cause: ")
                    .append(rootCause.getClass().getName());

            // log exception message
            if (StringUtils.isNotBlank(rootCause.getMessage())) {
                messageBuilder
                        .append("('")
                        .append(rootCause.getMessage())
                        .append("')");
            }
        }

        String stackTrace = Arrays.stream(t.getStackTrace())
                .filter(frame -> OMITTED_CLASS_PREFIXES.stream().noneMatch(frame.getClassName()::startsWith))
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n    at "));

        messageBuilder
                .append(". Stack trace with trivial frames omitted:\n    ")
                .append(stackTrace);

        return messageBuilder.toString();
    }

}
