package guru.nicks.commons.cucumber.validation;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.validation.dsl.CollectionValidationContext;
import guru.nicks.commons.validation.dsl.DoubleValidationContext;
import guru.nicks.commons.validation.dsl.InstantValidationContext;
import guru.nicks.commons.validation.dsl.IntegerValidationContext;
import guru.nicks.commons.validation.dsl.LongValidationContext;
import guru.nicks.commons.validation.dsl.StringValidationContext;
import guru.nicks.commons.validation.dsl.ValiDsl;
import guru.nicks.commons.validation.dsl.ValidationContext;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static guru.nicks.commons.validation.dsl.ValiDsl.check;
import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Step definitions for testing {@link ValiDsl} functionality.
 */
@RequiredArgsConstructor
public class ValiDslSteps2 {

    // DI
    private final TextWorld textWorld;

    private String stringValue;
    private Integer integerValue;
    private Long longValue;
    private Double doubleValue;
    private List<CollectionItem> collectionValue;

    private Object genericObject;
    private String returnedValue;

    private InstantValidationContext instantValidationContext;
    private Instant otherInstant;

    @DataTableType
    public CollectionItem createCollectionItem(Map<String, String> entry) {
        return CollectionItem.builder()
                .item(entry.get("item"))
                .build();
    }

    @Given("a string value {string}")
    public void aStringValue(String value) {
        stringValue = value;
    }

    @Given("a null string value")
    public void aNullStringValue() {
        stringValue = null;
    }

    @Given("an integer value {int}")
    public void anIntegerValue(Integer value) {
        integerValue = value;
    }

    @Given("a long value {int}")
    public void aLongValue(Integer value) {
        longValue = value.longValue();
    }

    @Given("a double value {double}")
    public void aDoubleValue(Double value) {
        doubleValue = value;
    }

    @Given("a collection with items:")
    public void aCollectionWithItems(List<CollectionItem> items) {
        collectionValue = items;
    }

    @Given("a generic object")
    public void aGenericObject() {
        genericObject = new Object();
    }

    @Given("a null generic object")
    public void aNullGenericObject() {
        genericObject = null;
    }

    @When("the string is validated to be not null")
    public void theStringIsValidatedToBeNotNull() {
        textWorld.setLastException(catchThrowable(() ->
                check(stringValue, "user.name").notNull()));
    }

    @When("the string is validated to be not blank")
    public void theStringIsValidatedToBeNotBlank() {
        textWorld.setLastException(catchThrowable(() ->
                checkNotBlank(stringValue, "user.name")));
    }

    @When("the string length is validated to be between {int} and {int}")
    public void theStringLengthIsValidatedToBeBetween(int min, int max) {
        textWorld.setLastException(catchThrowable(() -> {
            StringValidationContext context = ValiDsl.check(stringValue, "user.name");
            context.lengthBetweenInclusive(min, max);
        }));
    }

    @When("the string is validated with a custom predicate for alphanumeric")
    public void theStringIsValidatedWithACustomPredicateForAlphanumeric() {
        textWorld.setLastException(catchThrowable(() -> {
            Pattern alphanumeric = Pattern.compile("^[a-zA-Z0-9]*$");
            Predicate<String> isAlphanumeric = s -> alphanumeric.matcher(s).matches();

            StringValidationContext context = check(stringValue, "user.name").notNull();
            context.constraint(isAlphanumeric, "must be alphanumeric");
        }));
    }

    @When("the integer is validated to be greater than {int}")
    public void theIntegerIsValidatedToBeGreaterThan(int threshold) {
        textWorld.setLastException(catchThrowable(() -> {
            IntegerValidationContext context = ValiDsl.check(integerValue, "user.age");
            context.greaterThan(threshold);
        }));
    }

    @When("the integer is validated to be between {int} and {int}")
    public void theIntegerIsValidatedToBeBetween(int min, int max) {
        textWorld.setLastException(catchThrowable(() -> {
            IntegerValidationContext context = ValiDsl.check(integerValue, "user.age");
            context.betweenInclusive(min, max);
        }));
    }

    @When("the integer is validated with multiple custom predicates")
    public void theIntegerIsValidatedWithMultipleCustomPredicates() {
        textWorld.setLastException(catchThrowable(() -> {
            IntegerValidationContext context = check(integerValue, "user.age").notNull();
            context.constraint(i -> i > 0, "must be positive")
                    .constraint(i -> i < 10, "must be less than 10");
        }));
    }

    @When("the collection is validated to be not empty")
    public void theCollectionIsValidatedToBeNotEmpty() {
        textWorld.setLastException(catchThrowable(() ->
                check(collectionValue, "user.items").notEmpty()));
    }

    @When("the collection size is validated to be between {int} and {int}")
    public void theCollectionSizeIsValidatedToBeBetween(int min, int max) {
        textWorld.setLastException(catchThrowable(() -> {
            CollectionValidationContext<List<CollectionItem>> context = ValiDsl.check(collectionValue, "user.items");
            context.sizeBetweenInclusive(min, max);
        }));
    }

    @When("the long is validated to be greater than {int}")
    public void theLongIsValidatedToBeGreaterThan(int threshold) {
        textWorld.setLastException(catchThrowable(() -> {
            LongValidationContext context = check(longValue, "user.count");
            context.greaterThan((long) threshold);
        }));
    }

    @When("the double is validated to be between {double} and {double}")
    public void theDoubleIsValidatedToBeBetween(double min, double max) {
        textWorld.setLastException(catchThrowable(() -> {
            DoubleValidationContext context = ValiDsl.check(doubleValue, "user.rate");
            context.betweenInclusive(min, max);
        }));
    }

    @When("the object is validated to be not null")
    public void theObjectIsValidatedToBeNotNull() {
        textWorld.setLastException(catchThrowable(() -> {
            ValidationContext<Object> context = ValiDsl.check(genericObject, "user.data");
            context.notNull();
        }));
    }

    @When("validations for not null and minimum length are chained")
    public void validationsForNotNullAndMinimumLengthAreChained() {
        textWorld.setLastException(catchThrowable(() -> {
            StringValidationContext context = ValiDsl.check(stringValue, "user.name");
            context.notNull()
                    .longerThanOrEqual(2);
        }));
    }

    @When("the string is validated and the value is retrieved")
    public void theStringIsValidatedAndTheValueIsRetrieved() {
        textWorld.setLastException(catchThrowable(() -> {
            StringValidationContext context = check(stringValue, "user.name").notNull();
            returnedValue = context.getValue();
        }));
    }

    @When("the string is validated with custom error message")
    public void theStringIsValidatedWithCustomErrorMessage() {
        textWorld.setLastException(catchThrowable(() -> {
            StringValidationContext context = ValiDsl.check(stringValue, "user.name");
            context.constraint(s -> s != null && !s.isEmpty(), "Custom error: %s is empty");
        }));
    }

    @When("the string is validated with a delegate validator")
    public void theStringIsValidatedWithADelegateValidator() {
        textWorld.setLastException(catchThrowable(() -> {
            Consumer<StringValidationContext> lengthValidator = ctx -> ctx.longerThanOrEqual(2);

            StringValidationContext context = check(stringValue, "user.name").notNull();
            context.constraint(lengthValidator);
        }));
    }

    @When("the string is validated with complex chained validations")
    public void theStringIsValidatedWithComplexChainedValidations() {
        textWorld.setLastException(catchThrowable(() -> {
            Pattern alphanumeric = Pattern.compile("^[a-zA-Z0-9]*$");
            Predicate<String> isAlphanumeric = s -> alphanumeric.matcher(s).matches();

            check(stringValue, "user.name")
                    .notBlank()
                    .longerThanOrEqual(2)
                    .shorterThanOrEqual(20)
                    .constraint(isAlphanumeric, "must be alphanumeric");
        }));
    }

    @Then("the validation should pass")
    public void theValidationShouldPass() {
        assertThat(textWorld.getLastException())
                .as("Validation should pass (no exception thrown)")
                .isNull();
    }

    @Then("the validation should fail with message {string}")
    public void theValidationShouldFailWithMessage(String expectedMessage) {
        assertThat(textWorld.getLastException())
                .as("Exception should be thrown")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    @Then("the returned value should be {string}")
    public void theReturnedValueShouldBe(String expected) {
        assertThat(textWorld.getLastException()).as("No exception should be thrown").isNull();
        assertThat(returnedValue).as("Returned value").isEqualTo(expected);
    }

    /**
     * Creates an {@link InstantValidationContext} from a possibly blank value string.
     *
     * @param name       value name to be used in validation messages.
     * @param instantStr ISO-8601 string or blank to represent null.
     */
    @Given("an Instant named {string} with value {string}")
    public void givenAnInstantNamedWithValue(String name, String instantStr) {
        Instant value = StringUtils.isNotBlank(instantStr)
                ? Instant.parse(instantStr.strip())
                : null;

        instantValidationContext = new InstantValidationContext(value, name);
        assertThat(instantValidationContext)
                .as("context")
                .isNotNull();
    }

    /**
     * Sets the 'other' {@link Instant} from a possibly blank string.
     *
     * @param otherInstantStr ISO-8601 string or blank to represent null.
     */
    @And("other Instant value {string}")
    public void andOtherInstantValue(String otherInstantStr) {
        otherInstant = StringUtils.isNotBlank(otherInstantStr)
                ? Instant.parse(otherInstantStr.strip())
                : null;
    }

    /**
     * Performs isBefore validation while capturing the last exception in {@link TextWorld}.
     */
    @When("'before' validation is performed")
    public void whenIsBeforeValidationIsPerformed() {
        Throwable thrown = catchThrowable(() -> instantValidationContext.before(otherInstant));
        textWorld.setLastException(thrown);
    }

    @When("'before or equal' validation is performed")
    public void beforeOrEqualValidationIsPerformed() {
        Throwable thrown = catchThrowable(() -> instantValidationContext.beforeOrEqual(otherInstant));
        textWorld.setLastException(thrown);
    }

    /**
     * Performs isAfter validation while capturing the last exception in {@link TextWorld}.
     */
    @When("'after' validation is performed")
    public void whenIsAfterValidationIsPerformed() {
        Throwable thrown = catchThrowable(() -> instantValidationContext.after(otherInstant));
        textWorld.setLastException(thrown);
    }

    @When("'after or equal' validation is performed")
    public void afterOrEqualValidationIsPerformed() {
        Throwable thrown = catchThrowable(() -> instantValidationContext.afterOrEqual(otherInstant));
        textWorld.setLastException(thrown);
    }

    @Value
    @Builder
    public static class CollectionItem {

        String item;

    }

}
