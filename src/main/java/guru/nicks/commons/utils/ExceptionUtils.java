package guru.nicks.commons.utils;

import guru.nicks.commons.exception.BusinessException;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
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

    private static final Cache<
            Class<? extends RuntimeException>,
            Function<Throwable, RuntimeException>> EXCEPTION_FACTORY_CACHE = Caffeine.newBuilder().build();

    private static final Cache<
            Class<? extends BusinessException>,
            Function<Throwable, BusinessException>> BUSINESS_EXCEPTION_FACTORY_CACHE = Caffeine.newBuilder().build();

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

    /**
     * Starting with Spring Boot 3.5.3, controller exceptions are often wrapped in {@link InvocationTargetException}
     * (more than once!); it bears no business meaning and hides the original exception. Likewise, reflection
     * ({@link Method#invoke(Object, Object...)}) hides the original exception behind
     * {@link InvocationTargetException}.
     *
     * @param cause original exception
     * @return unwrapped exception (hidden behind multiple {@link InvocationTargetException}'s, up to 100 of them)
     */
    public static Throwable unwrapInvocationTargetException(Throwable cause) {
        int depth = 0;
        int maxDepth = 100;

        // limit maximum depth to work around circular references
        while ((cause instanceof InvocationTargetException ite)
                && (ite.getTargetException() != null)
                && (++depth <= maxDepth)) {
            cause = ite.getTargetException();
        }

        return cause;
    }

    /**
     * Rethrows the given exception as-is, bypassing the compiler's checked-exception checking (the 'sneaky throw'
     * idiom). Provides exception transparency: an exception caught in a generic context (such as reflection) reaches
     * the caller unchanged, without wrapping. If the argument is {@code null}, the resulting {@code throw null}
     * yields {@link NullPointerException}, mirroring plain Java semantics.
     *
     * @param t exception to rethrow
     * @param <T> exception type inferred at the call site, enabling the {@code throw sneakyThrow(t)} idiom
     * @return never returns normally
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    /**
     * Returns a cached exception factory function (or creates it) for the given exception class.
     *
     * @param exceptionClass exception class
     * @return factory that accepts a cause ({@link Throwable}) and creates instances of the exception class
     * @throws IllegalStateException if the exception class does not have a public constructor with a cause parameter
     *                               (so the class itself must be public too)
     */
    public static Function<Throwable, RuntimeException> getExceptionFactory(
            Class<? extends RuntimeException> exceptionClass) {
        return EXCEPTION_FACTORY_CACHE.get(exceptionClass, ExceptionUtils::getExceptionFactoryWithoutCache);
    }

    /**
     * Returns a cached exception factory function (or creates it) for the given business exception class.
     *
     * @param exceptionClass business exception class
     * @return factory that accepts a cause ({@link Throwable}) and creates instances of the exception class
     * @throws IllegalStateException if the exception class does not have a public constructor with a cause parameter
     *                               (so the class itself must be public too)
     */
    public static Function<Throwable, BusinessException> getBusinessExceptionFactory(
            Class<? extends BusinessException> exceptionClass) {
        return BUSINESS_EXCEPTION_FACTORY_CACHE.get(exceptionClass,
                ExceptionUtils::getBusinessExceptionFactoryWithoutCache);
    }

    private static Function<Throwable, RuntimeException> getExceptionFactoryWithoutCache(
            Class<? extends RuntimeException> exceptionClass) {
        MethodHandle constructorHandle = getConstructorHandleWithCause(exceptionClass);

        return cause -> {
            try {
                return (RuntimeException) constructorHandle.invoke(cause);
            }
            // from Javadoc: 'anything thrown by the underlying method propagates unchanged'
            catch (Throwable e) {
                throw new IllegalStateException("Error instantiating exception ["
                        + exceptionClass.getName() + "]: " + e.getMessage(), e);
            }
        };
    }

    private static Function<Throwable, BusinessException> getBusinessExceptionFactoryWithoutCache(
            Class<? extends BusinessException> exceptionClass) {
        MethodHandle constructorHandle = getConstructorHandleWithCause(exceptionClass);

        return cause -> {
            try {
                return (BusinessException) constructorHandle.invoke(cause);
            }
            // from Javadoc: 'anything thrown by the underlying method propagates unchanged'
            catch (Throwable e) {
                throw new IllegalStateException("Error instantiating exception ["
                        + exceptionClass.getName() + "]: " + e.getMessage(), e);
            }
        };
    }

    private static MethodHandle getConstructorHandleWithCause(Class<? extends RuntimeException> exceptionClass) {
        // faster than reflection - see e.g. https://dev.java/learn/introduction_to_method_handles/
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodType constructorType = MethodType.methodType(void.class, Throwable.class);

        try {
            return lookup.findConstructor(exceptionClass, constructorType);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Exception class [" + exceptionClass.getName()
                    + "] must have a public constructor accepting a (nullable) Throwable, but: " + e.getMessage(),
                    e);
        }
    }

}
