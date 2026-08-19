package guru.nicks.commons.cucumber.exception;

import guru.nicks.commons.exception.BusinessException;

/**
 * Minimal {@link BusinessException} fixture exposing the public {@code (Throwable)} constructor required by
 * {@code ExceptionUtils.getBusinessExceptionFactory(Class)}.
 */
public class TestBusinessException extends BusinessException {

    public TestBusinessException(Throwable cause) {
        super(cause);
    }

}
