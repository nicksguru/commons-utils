package guru.nicks.commons.exception;

import guru.nicks.commons.utils.ExceptionUtils;

import jakarta.annotation.Nullable;

import java.util.function.Function;

/**
 * Remote services throw exceptions which are rendered in JSON responses with business error codes. Each error code
 * points to a {@link BusinessException} in order to map it back to the original exception (or to its closest parent
 * listed here). This is tight coupling, but it ensures that a target exception exists for each error code and
 * <b>remote errors are seen as local exceptions</b>.
 * <p>
 * Implementors are typically error code enums where each constant is mapped to a specific {@link BusinessException}
 * subclass.
 * <p>
 * <b>These error codes are exposed to the client side</b> - to display meaningful, preferably localized, error
 * messages and react to them appropriately. Not all errors are safe to expose, hence 'business errors' vs. 'technical
 * errors'.
 * <p>
 * <b>Implementation requirements:</b> the mapped exception class must have a public constructor accepting a (nullable)
 * {@link Throwable}.
 * <p>
 * <b>Typical usage:</b> {@code throw MY_ERROR_CODE.toException()} or {@code throw MY_ERROR_CODE.toException(cause)}
 * when the original cause is available.
 */
public interface BusinessExceptionProvider {

    /**
     * Returns a function that creates instances of {@link #getExceptionClass()}. Default implementation delegates to
     * {@link ExceptionUtils#getBusinessExceptionFactory(Class)}. Repeated lookups are absorbed by its cache.
     *
     * @return a function that accepts a cause ({@link Throwable}) and creates instances of the exception class
     */
    default Function<Throwable, BusinessException> getExceptionFactory() {
        return ExceptionUtils.getBusinessExceptionFactory(getExceptionClass());
    }

    /**
     * Returns the exception class mapped to this error code. The reverse mapping is unique too: one exception class
     * cannot have multiple error codes.
     *
     * @return the exception class mapped to this error code
     */
    Class<? extends BusinessException> getExceptionClass();

    /**
     * Shortcut to {@link #toException(Throwable)} passing {@code null} as the argument.
     *
     * @return a new instance of the exception class mapped to this error code
     * @throws IllegalStateException error invoking a constructor accepting a {@link Throwable}
     */
    default BusinessException toException() {
        return toException(null);
    }

    /**
     * Creates a new instance of {@link #getExceptionClass()} with the given cause.
     *
     * @param cause exception cause, can be {@code null}
     * @return a new instance of the exception class
     * @throws IllegalStateException error invoking a constructor accepting a {@link Throwable}
     */
    default BusinessException toException(@Nullable Throwable cause) {
        return getExceptionFactory().apply(cause);
    }

}
