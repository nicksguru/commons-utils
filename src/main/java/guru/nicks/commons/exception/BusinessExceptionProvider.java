package guru.nicks.commons.exception;

import jakarta.annotation.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
 * {@link Throwable}. The exception factory should be created once via {@link #createExceptionFactory(Class)} and cached
 * (e.g., stored in a field when the enum constant is constructed) rather than recreated on every
 * {@link #getExceptionFactory()} call.
 * <p>
 * <b>Typical usage:</b> {@code throw MY_ERROR_CODE.toException()} or {@code throw MY_ERROR_CODE.toException(cause)}
 * when the original cause is available.
 */
public interface BusinessExceptionProvider {

    /**
     * Returns a function that creates instances of the exception class mapped to this error code. This is supposed to
     * be the result of {@link #createExceptionFactory(Class)} stored in a variable.
     *
     * @return a function that accepts a cause ({@link Throwable}) and creates instances of the exception class
     */
    Function<Throwable, BusinessException> getExceptionFactory();

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
     * Creates a new instance of {@link #getExceptionClass()} with the given cause by calling
     * {@link #getExceptionFactory()}.
     *
     * @param cause exception cause, can be {@code null}
     * @return a new instance of the exception class mapped to this error code
     * @throws IllegalStateException error invoking a constructor accepting a {@link Throwable}
     */
    default BusinessException toException(@Nullable Throwable cause) {
        return getExceptionFactory().apply(cause);
    }

    /**
     * Creates an exception factory function for the given exception class. This is actually the exception constructor
     * wrapped in a {@link Function}.
     *
     * @param exceptionClass exception class
     * @return factory that accepts a cause ({@link Throwable}) and creates instances of the exception class
     */
    default Function<Throwable, BusinessException> createExceptionFactory(Class<? extends Throwable> exceptionClass) {
        // faster than reflection - see e.g. https://dev.java/learn/introduction_to_method_handles/
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodType constructorType = MethodType.methodType(void.class, Throwable.class);
        MethodHandle constructorHandle;

        try {
            constructorHandle = lookup.findConstructor(exceptionClass, constructorType);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Exception class [" + exceptionClass.getName()
                    + "] must have a public constructor accepting a (nullable) Throwable, but: " + e.getMessage(), e);
        }

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

}
