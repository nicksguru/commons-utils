package guru.nicks.commons.cucumber.validation;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.validation.dsl.StringValidationContext;
import guru.nicks.commons.validation.dsl.ValiDsl;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for comprehensively testing {@link StringValidationContext} (created via {@link ValiDsl#check}).
 * Each {@code @When} step runs a single validation, capturing any thrown exception into {@link TextWorld}; the
 * {@code @Then} steps then assert on the captured exception.
 */
@RequiredArgsConstructor
@Slf4j
public class StringValidationContextSteps {

    // field name used consistently across all validation messages
    private static final String FIELD_NAME = "user.name";

    // literal marker in feature files that represents a Java null value
    private static final String NULL_MARKER = "null";

    // DI
    private final TextWorld textWorld;

    // current value under test (may be null)
    private String stringValue;

    /**
     * Stores the string value under test, converting the literal {@code "null"} marker into Java {@code null}.
     *
     * @param value string value (or the {@code null} marker)
     */
    @Given("the string value is {string}")
    public void theStringValueIs(String value) {
        // treat the literal marker as Java null
        stringValue = NULL_MARKER.equals(value)
                ? null
                : value;
    }

    /**
     * Stores a Java {@code null} as the value under test.
     */
    @Given("the string value is null")
    public void theStringValueIsNull() {
        stringValue = null;
    }

    /**
     * Verifies that a blank field name is rejected by the context constructor.
     */
    @When("the string is checked with a blank name")
    public void theStringIsCheckedWithABlankName() {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, "   ")));
    }

    /**
     * Runs {@link StringValidationContext#notNull()}.
     */
    @When("the string is checked with notNull")
    public void theStringIsCheckedWithNotNull() {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).notNull()));
    }

    /**
     * Runs {@link StringValidationContext#notEmpty()}.
     */
    @When("the string is checked with notEmpty")
    public void theStringIsCheckedWithNotEmpty() {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).notEmpty()));
    }

    /**
     * Runs {@link StringValidationContext#notBlank()}.
     */
    @When("the string is checked with notBlank")
    public void theStringIsCheckedWithNotBlank() {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).notBlank()));
    }

    /**
     * Runs {@link StringValidationContext#shorterThan(int)}.
     *
     * @param threshold maximum exclusive length
     */
    @When("the string is checked with shorterThan {int}")
    public void theStringIsCheckedWithShorterThan(int threshold) {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).shorterThan(threshold)));
    }

    /**
     * Runs {@link StringValidationContext#shorterThanOrEqual(int)}.
     *
     * @param threshold maximum inclusive length
     */
    @When("the string is checked with shorterThanOrEqual {int}")
    public void theStringIsCheckedWithShorterThanOrEqual(int threshold) {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).shorterThanOrEqual(threshold)));
    }

    /**
     * Runs {@link StringValidationContext#longerThan(int)}.
     *
     * @param threshold minimum exclusive length
     */
    @When("the string is checked with longerThan {int}")
    public void theStringIsCheckedWithLongerThan(int threshold) {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).longerThan(threshold)));
    }

    /**
     * Runs {@link StringValidationContext#longerThanOrEqual(int)}.
     *
     * @param threshold minimum inclusive length
     */
    @When("the string is checked with longerThanOrEqual {int}")
    public void theStringIsCheckedWithLongerThanOrEqual(int threshold) {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).longerThanOrEqual(threshold)));
    }

    /**
     * Runs {@link StringValidationContext#lengthBetweenInclusive(int, int)}.
     *
     * @param min minimum inclusive length
     * @param max maximum inclusive length
     */
    @When("the string is checked with lengthBetweenInclusive {int} and {int}")
    public void theStringIsCheckedWithLengthBetweenInclusive(int min, int max) {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).lengthBetweenInclusive(min, max)));
    }

    /**
     * Runs {@link StringValidationContext#startsWith(String)}. The literal {@code "null"} marker is
     * converted to Java {@code null} so that null-argument rejection can be exercised.
     *
     * @param prefix expected prefix (or the {@code null} marker)
     */
    @When("the string is checked with startsWith {string}")
    public void theStringIsCheckedWithStartsWith(String prefix) {
        // treat the literal marker as Java null
        var resolved = NULL_MARKER.equals(prefix)
                ? null
                : prefix;
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).startsWith(resolved)));
    }

    /**
     * Runs {@link StringValidationContext#endsWith(String)}. The literal {@code "null"} marker is
     * converted to Java {@code null} so that null-argument rejection can be exercised.
     *
     * @param suffix expected suffix (or the {@code null} marker)
     */
    @When("the string is checked with endsWith {string}")
    public void theStringIsCheckedWithEndsWith(String suffix) {
        // treat the literal marker as Java null
        var resolved = NULL_MARKER.equals(suffix)
                ? null
                : suffix;
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).endsWith(resolved)));
    }

    /**
     * Runs {@link StringValidationContext#contains(String)}. The literal {@code "null"} marker is
     * converted to Java {@code null} so that null-argument rejection can be exercised.
     *
     * @param substring expected substring (or the {@code null} marker)
     */
    @When("the string is checked with contains {string}")
    public void theStringIsCheckedWithContains(String substring) {
        // treat the literal marker as Java null
        var resolved = NULL_MARKER.equals(substring)
                ? null
                : substring;
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).contains(resolved)));
    }

    /**
     * Runs a custom predicate that always fails, using the given message template.
     *
     * @param message custom error message template
     */
    @When("the string is checked with a failing custom predicate and message {string}")
    public void theStringIsCheckedWithAFailingCustomPredicate(String message) {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).constraint(s -> false, message)));
    }

    /**
     * Runs a custom predicate that always passes.
     */
    @When("the string is checked with a passing custom predicate")
    public void theStringIsCheckedWithAPassingCustomPredicate() {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).constraint(s -> true, "should never be reported")));
    }

    /**
     * Runs a failing custom predicate with a {@code null} message template, exercising the default {@code %s}
     * fallback.
     */
    @When("the string is checked with a custom predicate and a null message")
    public void theStringIsCheckedWithACustomPredicateAndNullMessage() {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME).constraint(s -> false, null)));
    }

    /**
     * Runs a chain of validations that all pass for a valid value.
     */
    @When("the string is checked with chained validations that all pass")
    public void theStringIsCheckedWithChainedValidationsThatAllPass() {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME)
                        .notNull()
                        .notEmpty()
                        .longerThanOrEqual(3)
                        .shorterThanOrEqual(10)));
    }

    /**
     * Runs a chain where the first validation ({@code notNull}) fails immediately (short-circuit).
     */
    @When("the string is checked with chained validations where the first fails")
    public void theStringIsCheckedWithChainedValidationsWhereTheFirstFails() {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME)
                        .notNull()
                        .notEmpty()
                        .longerThanOrEqual(3)));
    }

    /**
     * Runs a chain where the first validation passes but the second ({@code longerThanOrEqual}) fails.
     */
    @When("the string is checked with chained validations where the second fails")
    public void theStringIsCheckedWithChainedValidationsWhereTheSecondFails() {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, FIELD_NAME)
                        .notNull()
                        .longerThanOrEqual(3)));
    }

    /**
     * Asserts that the previous validation passed (no exception was captured).
     */
    @Then("the check should pass")
    public void theCheckShouldPass() {
        assertThat(textWorld.getLastException())
                .as("Validation should pass (no exception thrown)")
                .isNull();
    }

    /**
     * Asserts that the previous validation failed with the exact expected message.
     *
     * @param expectedMessage exact exception message
     */
    @Then("the check should fail with message {string}")
    public void theCheckShouldFailWithMessage(String expectedMessage) {
        assertThat(textWorld.getLastException())
                .as("Exception should be thrown")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }
}
