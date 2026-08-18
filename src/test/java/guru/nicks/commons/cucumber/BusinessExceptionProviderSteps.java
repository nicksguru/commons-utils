package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.exception.TestErrorCode;
import guru.nicks.commons.cucumber.exception.TestErrorCode.NoThrowableConstructorException;
import guru.nicks.commons.cucumber.exception.TestErrorCode.NonPublicConstructorException;
import guru.nicks.commons.cucumber.exception.TestErrorCode.ThrowingConstructorException;
import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.exception.AlreadyExistsException;
import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.exception.BusinessExceptionProvider;
import guru.nicks.commons.exception.http.ConflictException;
import guru.nicks.commons.exception.http.NotFoundException;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for {@link BusinessExceptionProvider} testing.
 */
@RequiredArgsConstructor
public class BusinessExceptionProviderSteps {

    /**
     * Exception classes resolvable by their simple names in feature files (including fixtures violating the
     * implementation requirements on purpose).
     */
    private static final Map<String, Class<? extends Throwable>> EXCEPTION_CLASSES = Map.of(
            "BusinessException", BusinessException.class,
            "NotFoundException", NotFoundException.class,
            "ConflictException", ConflictException.class,
            "AlreadyExistsException", AlreadyExistsException.class,
            "NoThrowableConstructorException", NoThrowableConstructorException.class,
            "NonPublicConstructorException", NonPublicConstructorException.class,
            "ThrowingConstructorException", ThrowingConstructorException.class);

    // DI
    private final TextWorld textWorld;

    private TestErrorCode errorCode;
    private Throwable originalCause;

    private BusinessException createdException;
    private BusinessException anotherCreatedException;
    private Function<Throwable, BusinessException> createdFactory;

    @Given("error code {word} is used")
    public void errorCodeIsUsed(String errorCodeName) {
        errorCode = TestErrorCode.valueOf(errorCodeName);
    }

    @When("the exception is created without a cause")
    public void theExceptionIsCreatedWithoutACause() {
        textWorld.setLastException(catchThrowable(() -> createdException = errorCode.toException()));
    }

    @When("the exception is created with a cause")
    public void theExceptionIsCreatedWithACause() {
        originalCause = new IllegalStateException("Original remote error");
        textWorld.setLastException(catchThrowable(() ->
                createdException = errorCode.toException(originalCause)));
    }

    @When("the exception is created {int} times")
    public void theExceptionIsCreatedTimes(int times) {
        textWorld.setLastException(catchThrowable(() -> {
            for (int i = 0; i < times; i++) {
                // keep the first and the last created exceptions for comparison
                if (createdException == null) {
                    createdException = errorCode.toException();
                } else {
                    anotherCreatedException = errorCode.toException();
                }
            }
        }));
    }

    @When("an exception factory is created for {word}")
    public void anExceptionFactoryIsCreatedFor(String exceptionSimpleName) {
        Class<? extends Throwable> exceptionClass = resolveExceptionClass(exceptionSimpleName);

        textWorld.setLastException(catchThrowable(() ->
                createdFactory = errorCode.createExceptionFactory(exceptionClass)));
    }

    @When("the created exception factory is invoked")
    public void theCreatedExceptionFactoryIsInvoked() {
        textWorld.setLastException(catchThrowable(() ->
                createdException = createdFactory.apply(null)));
    }

    @Then("a new exception of type {word} should be created")
    public void aNewExceptionOfTypeShouldBeCreated(String exceptionSimpleName) {
        Class<? extends Throwable> expectedClass = resolveExceptionClass(exceptionSimpleName);

        assertThat(createdException)
                .as("created exception")
                .isNotNull()
                .isInstanceOf(expectedClass);
    }

    @Then("the created exception cause should be null")
    public void theCreatedExceptionCauseShouldBeNull() {
        assertThat(createdException.getCause())
                .as("created exception cause")
                .isNull();
    }

    @Then("the created exception cause should be the original cause")
    public void theCreatedExceptionCauseShouldBeTheOriginalCause() {
        assertThat(createdException.getCause())
                .as("created exception cause")
                .isSameAs(originalCause);
    }

    @Then("each created exception should be a new instance")
    public void eachCreatedExceptionShouldBeANewInstance() {
        assertThat(anotherCreatedException)
                .as("another created exception")
                .isNotNull()
                .isNotSameAs(createdException);
    }

    @Then("the mapped exception class should be {word}")
    public void theMappedExceptionClassShouldBe(String exceptionSimpleName) {
        assertThat(errorCode.getExceptionClass())
                .as("mapped exception class")
                .isEqualTo(resolveExceptionClass(exceptionSimpleName));
    }

    @Then("the exception factory should be reused")
    public void theExceptionFactoryShouldBeReused() {
        assertThat(errorCode.getExceptionFactory())
                .as("exception factory")
                .isSameAs(errorCode.getExceptionFactory());
    }

    /**
     * Resolves an exception class by its simple name, failing early on an unknown fixture name.
     *
     * @param exceptionSimpleName exception class simple name
     * @return resolved exception class
     */
    private Class<? extends Throwable> resolveExceptionClass(String exceptionSimpleName) {
        Class<? extends Throwable> exceptionClass = EXCEPTION_CLASSES.get(exceptionSimpleName);

        assertThat(exceptionClass)
                .as("exception class fixture '" + exceptionSimpleName + "'")
                .isNotNull();

        return exceptionClass;
    }

}
