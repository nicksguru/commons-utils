package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.exception.NoCauseConstructorException;
import guru.nicks.commons.cucumber.exception.TestBusinessException;
import guru.nicks.commons.cucumber.exception.TestErrorCode.NoThrowableConstructorException;
import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.exception.BusinessException;
import guru.nicks.commons.utils.ExceptionUtils;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for {@link ExceptionUtils} testing.
 */
@RequiredArgsConstructor
public class ExceptionUtilsSteps {

    /**
     * Exception classes resolvable by their simple names in feature files (for the exception factory).
     */
    private static final Map<String, Class<? extends RuntimeException>> FACTORY_EXCEPTION_CLASSES = Map.of(
            "IllegalStateException", IllegalStateException.class,
            "NoCauseConstructorException", NoCauseConstructorException.class);

    /**
     * Business exception classes resolvable by their simple names in feature files (for the business exception
     * factory), including a fixture violating the constructor requirement on purpose.
     */
    private static final Map<String, Class<? extends BusinessException>> FACTORY_BUSINESS_EXCEPTION_CLASSES = Map.of(
            "TestBusinessException", TestBusinessException.class,
            "NoThrowableConstructorException", NoThrowableConstructorException.class);

    // DI
    private final TextWorld textWorld;

    private Throwable testException;

    private Throwable originalCause;
    private Throwable unwrappedException;
    private List<Throwable> wrapperChain;

    private Class<? extends RuntimeException> exceptionClass;
    private Class<? extends BusinessException> businessExceptionClass;

    private Function<Throwable, RuntimeException> exceptionFactory;
    private Function<Throwable, RuntimeException> anotherExceptionFactory;

    private Function<Throwable, BusinessException> businessExceptionFactory;
    private Function<Throwable, BusinessException> anotherBusinessExceptionFactory;

    private Throwable factoryCreatedException;

    @Given("an exception of type {string} with message {string} is created")
    public void anExceptionOfTypeWithMessageIsCreated(String exceptionType, String message) {
        var throwable = catchThrowable(() -> {
            try {
                Class<?> exceptionClass = Class.forName("java.lang." + exceptionType);
                Constructor<?> constructor = exceptionClass.getConstructor(String.class);
                testException = (Throwable) constructor.newInstance(
                        StringUtils.isNotBlank(message)
                                ? message
                                : null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create exception", e);
            }
        });

        textWorld.setLastException(throwable);
    }

    @Given("exception is null")
    public void exceptionIsNull() {
        testException = null;
    }

    @Given("exception has root cause of type {string} with message {string}")
    public void exceptionHasRootCauseOfTypeWithMessage(String rootCauseType, String rootCauseMessage) {
        var throwable = catchThrowable(() -> {
            try {
                Class<?> rootCauseClass = Class.forName("java.lang." + rootCauseType);
                Constructor<?> rootCauseConstructor = rootCauseClass.getConstructor(String.class);

                var rootCause = (Throwable) rootCauseConstructor.newInstance(
                        StringUtils.isNotBlank(rootCauseMessage)
                                ? rootCauseMessage
                                : null);

                // create a new exception with the root cause
                Class<?> exceptionClass = testException.getClass();
                Constructor<?> constructor = exceptionClass.getConstructor(String.class, Throwable.class);
                testException = (Throwable) constructor.newInstance(testException.getMessage(), rootCause);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create exception with root cause", e);
            }
        });

        textWorld.setLastException(throwable);
    }

    @Given("an exception with stack trace containing {string} is created")
    public void anExceptionWithStackTraceContainingIsCreated(String className) {
        var throwable = catchThrowable(() -> {
            var exception = new RuntimeException("Test exception");

            // Create a mock stack trace element with the specified class name
            var stackTraceElement = new StackTraceElement(
                    className,
                    "testMethod",
                    "TestFile.java",
                    42);

            // Add some real stack trace elements and the mock one
            var currentStackTrace = exception.getStackTrace();
            var newStackTrace = new StackTraceElement[currentStackTrace.length + 1];
            newStackTrace[0] = stackTraceElement;
            System.arraycopy(currentStackTrace, 0, newStackTrace, 1, currentStackTrace.length);

            exception.setStackTrace(newStackTrace);
            testException = exception;
        });

        textWorld.setLastException(throwable);
    }

    @When("exception is formatted with compact stack trace")
    public void exceptionIsFormattedWithCompactStackTrace() {
        var throwable = catchThrowable(() -> {
            String result = ExceptionUtils.formatWithCompactStackTrace(testException);
            textWorld.setOutput(result);
        });

        textWorld.setLastException(throwable);
    }

    @Then("output should contain exception class name {string}")
    public void outputShouldContainExceptionClassName(String exceptionType) {
        assertThat(textWorld.getOutput())
                .as("output")
                .isNotEmpty();

        assertThat(textWorld.getOutput().getFirst())
                .as("formatted exception output")
                .contains("java.lang." + exceptionType);
    }

    @And("output should contain message {string}")
    public void outputShouldContainMessage(String expectedMessage) {
        assertThat(textWorld.getOutput())
                .as("output")
                .isNotEmpty();

        assertThat(textWorld.getOutput().getFirst())
                .as("formatted exception output")
                .contains(expectedMessage);
    }

    @And("output should contain {string}")
    public void outputShouldContain(String expectedText) {
        assertThat(textWorld.getOutput())
                .as("output")
                .isNotEmpty();

        assertThat(textWorld.getOutput().getFirst())
                .as("formatted exception output")
                .contains(expectedText);
    }

    @And("output should contain root cause {string}")
    public void outputShouldContainRootCause(String expectedRootCause) {
        assertThat(textWorld.getOutput())
                .as("output")
                .isNotEmpty();

        assertThat(textWorld.getOutput().getFirst())
                .as("formatted exception output")
                .contains("with root cause: java.lang." + expectedRootCause);
    }

    @And("trivial frames should be omitted from stack trace")
    public void trivialFramesShouldBeOmittedFromStackTrace() {
        assertThat(textWorld.getOutput())
                .as("output")
                .isNotEmpty();

        String output = textWorld.getOutput().getFirst();

        // Verify that some common trivial frames are not present
        ExceptionUtils.OMITTED_CLASS_PREFIXES.forEach(prefix ->
                assertThat(output)
                        .as("formatted exception output should not contain trivial frame: " + prefix)
                        .doesNotContain(prefix));
    }

    @Then("stack trace {string} contain {string}")
    public void stackTraceShouldContain(String shouldContain, String className) {
        assertThat(textWorld.getOutput())
                .as("output")
                .isNotEmpty();

        String output = textWorld.getOutput().getFirst();

        switch (shouldContain) {
            case "should" -> assertThat(output)
                    .as("formatted exception output should contain: " + className)
                    .contains(className);

            case "should not" -> assertThat(output)
                    .as("formatted exception output should not contain: " + className)
                    .doesNotContain(className);

            default -> throw new IllegalArgumentException("Invalid should contain value: '" + shouldContain + "'");
        }
    }

    @Given("exception root cause is the exception itself")
    public void exceptionRootCauseIsTheExceptionItself() {
        // Apache Commons Lang returns the exception itself as its root cause when no cause is set
        testException = new IllegalStateException("Self-rooted");
    }

    /**
     * Wraps the current exception into the given number of nested {@link InvocationTargetException}'s, keeping the
     * whole chain (outermost first) to assert the unwrapping depth limit.
     *
     * @param depth number of nested wrappers
     */
    @Given("the exception is wrapped in {int} nested InvocationTargetExceptions")
    public void theExceptionIsWrappedInNestedInvocationTargetExceptions(int depth) {
        originalCause = testException;
        wrapperChain = new ArrayList<>(List.of(testException));

        for (int i = 0; i < depth; i++) {
            testException = new InvocationTargetException(testException);
            wrapperChain.addFirst(testException);
        }
    }

    @Given("the exception is wrapped in an InvocationTargetException without a target")
    public void theExceptionIsWrappedInAnInvocationTargetExceptionWithoutATarget() {
        // the no-arg constructor is protected, but the public two-arg one accepts a null target
        testException = new InvocationTargetException(null, "without a target");
    }

    @Given("an original cause exception is created")
    public void anOriginalCauseExceptionIsCreated() {
        originalCause = new IllegalStateException("Original cause");
    }

    @When("the exception is unwrapped from InvocationTargetException")
    public void theExceptionIsUnwrappedFromInvocationTargetException() {
        unwrappedException = ExceptionUtils.unwrapInvocationTargetException(testException);
    }

    @When("an exception factory is obtained for the {word} class")
    public void anExceptionFactoryIsObtainedForTheClass(String exceptionSimpleName) {
        exceptionClass = resolveExceptionClass(exceptionSimpleName);

        textWorld.setLastException(catchThrowable(() ->
                exceptionFactory = ExceptionUtils.getExceptionFactory(exceptionClass)));
    }

    @When("the exception factory is obtained again")
    public void theExceptionFactoryIsObtainedAgain() {
        textWorld.setLastException(catchThrowable(() ->
                anotherExceptionFactory = ExceptionUtils.getExceptionFactory(exceptionClass)));
    }

    @When("the exception factory is applied to the original cause")
    public void theExceptionFactoryIsAppliedToTheOriginalCause() {
        textWorld.setLastException(catchThrowable(() ->
                factoryCreatedException = exceptionFactory.apply(originalCause)));
    }

    @When("a business exception factory is obtained for the {word} class")
    public void aBusinessExceptionFactoryIsObtainedForTheClass(String exceptionSimpleName) {
        businessExceptionClass = resolveBusinessExceptionFactoryClass(exceptionSimpleName);

        textWorld.setLastException(catchThrowable(() ->
                businessExceptionFactory = ExceptionUtils.getBusinessExceptionFactory(businessExceptionClass)));
    }

    @When("the business exception factory is obtained again")
    public void theBusinessExceptionFactoryIsObtainedAgain() {
        textWorld.setLastException(catchThrowable(() ->
                anotherBusinessExceptionFactory = ExceptionUtils.getBusinessExceptionFactory(
                        businessExceptionClass)));
    }

    @When("the business exception factory is applied to the original cause")
    public void theBusinessExceptionFactoryIsAppliedToTheOriginalCause() {
        textWorld.setLastException(catchThrowable(() ->
                factoryCreatedException = businessExceptionFactory.apply(originalCause)));
    }

    @Then("the unwrapped exception should be the exception itself")
    public void theUnwrappedExceptionShouldBeTheExceptionItself() {
        assertThat(unwrappedException)
                .as("unwrapped exception")
                .isSameAs(testException);
    }

    @Then("the unwrapped exception should be the original cause")
    public void theUnwrappedExceptionShouldBeTheOriginalCause() {
        assertThat(unwrappedException)
                .as("unwrapped exception")
                .isSameAs(originalCause);
    }

    @Then("the unwrapped exception should not be the original cause")
    public void theUnwrappedExceptionShouldNotBeTheOriginalCause() {
        assertThat(unwrappedException)
                .as("unwrapped exception")
                .isNotSameAs(originalCause);
    }

    @Then("the unwrapped exception should be an InvocationTargetException")
    public void theUnwrappedExceptionShouldBeAnInvocationTargetException() {
        assertThat(unwrappedException)
                .as("unwrapped exception")
                .isInstanceOf(InvocationTargetException.class);
    }

    /**
     * The chain is limited to 100 unwrappings, so deeper chains stop at the exception reached after 100 unwrappings.
     *
     * @param depth expected number of performed unwrappings
     */
    @Then("the unwrapped exception should be the exception at unwrapping depth {int}")
    public void theUnwrappedExceptionShouldBeTheExceptionAtUnwrappingDepth(int depth) {
        assertThat(unwrappedException)
                .as("unwrapped exception at depth " + depth)
                .isSameAs(wrapperChain.get(depth));
    }

    @Then("the exception factory should be cached")
    public void theExceptionFactoryShouldBeCached() {
        assertThat(anotherExceptionFactory)
                .as("exception factory obtained repeatedly")
                .isSameAs(exceptionFactory);
    }

    @Then("the business exception factory should be cached")
    public void theBusinessExceptionFactoryShouldBeCached() {
        assertThat(anotherBusinessExceptionFactory)
                .as("business exception factory obtained repeatedly")
                .isSameAs(businessExceptionFactory);
    }

    @Then("the factory-created exception should be of type {word}")
    public void theFactoryCreatedExceptionShouldBeOfType(String exceptionSimpleName) {
        assertThat(factoryCreatedException)
                .as("factory-created exception")
                .isNotNull()
                .isInstanceOf(resolveFactoryClass(exceptionSimpleName));
    }

    @Then("the factory-created exception cause should be the original cause")
    public void theFactoryCreatedExceptionCauseShouldBeTheOriginalCause() {
        assertThat(factoryCreatedException.getCause())
                .as("factory-created exception cause")
                .isSameAs(originalCause);
    }

    @Then("the exception message should name the {word} class")
    public void theExceptionMessageShouldNameTheClass(String exceptionSimpleName) {
        assertThat(textWorld.getLastException())
                .as("lastException")
                .isNotNull();

        assertThat(textWorld.getLastException().getMessage())
                .as("lastException message")
                .contains(resolveFactoryClass(exceptionSimpleName).getName());
    }

    /**
     * Resolves an exception factory class by its simple name, failing early on an unknown fixture name.
     *
     * @param exceptionSimpleName exception class simple name
     * @return resolved exception class
     */
    private Class<? extends RuntimeException> resolveExceptionClass(String exceptionSimpleName) {
        Class<? extends RuntimeException> clazz = FACTORY_EXCEPTION_CLASSES.get(exceptionSimpleName);

        assertThat(clazz)
                .as("exception class fixture '" + exceptionSimpleName + "'")
                .isNotNull();

        return clazz;
    }

    /**
     * Resolves a business exception factory class by its simple name, failing early on an unknown fixture name.
     *
     * @param exceptionSimpleName business exception class simple name
     * @return resolved business exception class
     */
    private Class<? extends BusinessException> resolveBusinessExceptionFactoryClass(String exceptionSimpleName) {
        Class<? extends BusinessException> clazz = FACTORY_BUSINESS_EXCEPTION_CLASSES.get(
                exceptionSimpleName);

        assertThat(clazz)
                .as("business exception class fixture '" + exceptionSimpleName + "'")
                .isNotNull();

        return clazz;
    }

    /**
     * Resolves any factory fixture class (plain or business) by its simple name.
     *
     * @param exceptionSimpleName exception class simple name
     * @return resolved exception class
     */
    private Class<? extends Throwable> resolveFactoryClass(String exceptionSimpleName) {
        Class<? extends Throwable> clazz = FACTORY_EXCEPTION_CLASSES.get(exceptionSimpleName);

        if (clazz == null) {
            clazz = FACTORY_BUSINESS_EXCEPTION_CLASSES.get(exceptionSimpleName);
        }

        assertThat(clazz)
                .as("exception class fixture '" + exceptionSimpleName + "'")
                .isNotNull();

        return clazz;
    }

}
