package guru.nicks.commons.cucumber.exception;

/**
 * Plain checked exception fixture having no constructor accepting a {@link Throwable} - to test exception factory
 * creation failures.
 */
public class NoCauseConstructorException extends RuntimeException {

    public NoCauseConstructorException() {
        super();
    }

    public NoCauseConstructorException(String message) {
        super(message);
    }

}
