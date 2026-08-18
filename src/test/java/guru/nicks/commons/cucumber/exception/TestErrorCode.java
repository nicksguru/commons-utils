package guru.nicks.commons.cucumber.exception;

import guru.nicks.commons.exception.AlreadyExistsException;
import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.BusinessExceptionProvider;
import guru.nicks.commons.exception.http.ConflictException;
import guru.nicks.commons.exception.http.NotFoundException;

import lombok.Getter;

import java.util.function.Function;

/**
 * Error code enum simulating a typical error code enum of a remote service. Each constant is mapped to a specific
 * {@link BusinessException} subclass, following the implementation requirements of {@link BusinessExceptionProvider}:
 * the exception factory is created once (when the enum constant is constructed) and cached in a field.
 * <p>
 * The nested exception classes violate the implementation requirements on purpose - to test factory creation failures.
 */
public enum TestErrorCode implements BusinessExceptionProvider {

    /**
     * Mapped to a root HTTP status exception (404).
     */
    ENTITY_NOT_FOUND(NotFoundException.class),

    /**
     * Mapped to a root HTTP status exception (409).
     */
    DUPLICATE_ENTITY(ConflictException.class),

    /**
     * Mapped to a specific business case exception (inheriting the 409 status from its parent).
     */
    USER_ALREADY_EXISTS(AlreadyExistsException.class);

    @Getter(onMethod_ = @Override)
    private final Class<? extends BusinessException> exceptionClass;
    @Getter(onMethod_ = @Override)
    private final Function<Throwable, BusinessException> exceptionFactory;

    TestErrorCode(Class<? extends BusinessException> exceptionClass) {
        this.exceptionClass = exceptionClass;
        // created once and cached, as the interface Javadoc requires
        this.exceptionFactory = createExceptionFactory(exceptionClass);
    }

    /**
     * Has no constructor accepting a {@link Throwable} at all - to test the 'no such constructor' failure.
     */
    public static class NoThrowableConstructorException extends BusinessException {

        public NoThrowableConstructorException() {
            super();
        }

    }

    /**
     * Has only a package-private constructor accepting a {@link Throwable} - to test the 'inaccessible constructor'
     * failure.
     */
    public static class NonPublicConstructorException extends BusinessException {

        NonPublicConstructorException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * Has a public constructor accepting a {@link Throwable}, but it throws - to test wrapping of constructor
     * failures.
     */
    public static class ThrowingConstructorException extends BusinessException {

        public ThrowingConstructorException(Throwable cause) {
            super(cause);
            throw new IllegalStateException("Constructor failed on purpose");
        }

    }

}
