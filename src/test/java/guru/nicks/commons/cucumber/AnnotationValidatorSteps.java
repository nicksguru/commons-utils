package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.validation.AnnotationValidator;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@RequiredArgsConstructor
public class AnnotationValidatorSteps {

    // DI
    private final AnnotationValidator annotationValidator;
    private final TextWorld textWorld;

    private Object testObject;
    private String classNameResult;

    @Given("an object of type {string}")
    public void anObjectOfType(String className) throws Exception {
        testObject = getClass()
                .getClassLoader()
                .loadClass(className)
                .getDeclaredConstructor()
                .newInstance();
    }

    @When("the class name for binding result is retrieved")
    public void theClassNameForBindingResultIsRetrieved() {
        classNameResult = annotationValidator.getClassNameForBindingResult(testObject);
    }

    @Then("the class name should be {string}")
    public void theClassNameShouldBe(String expectedResult) {
        assertThat(classNameResult)
                .as("class name result")
                .isEqualTo(expectedResult);
    }

    @Given("a valid test object")
    public void aValidTestObject() {
        testObject = ValidTestObject.builder()
                .name("Valid Name")
                .build();
    }

    @Given("an invalid test object")
    public void anInvalidTestObject() {
        testObject = InvalidTestObject.builder()
                .name(null) // violates @NotBlank
                .build();
    }

    @Given("a test object with invalid nested property")
    public void aTestObjectWithInvalidNestedProperty() {
        var nestedObject = InvalidTestObject.builder()
                .name(null) // violates @NotBlank
                .build();

        testObject = ParentTestObject.builder()
                .child(nestedObject)
                .build();
    }

    @Given("a test object with circular reference")
    public void aTestObjectWithCircularReference() {
        var parent = CircularTestObject.builder()
                .name("Parent")
                .build();

        var child = CircularTestObject.builder()
                .name("Child")
                .build();

        parent.setReference(child);
        child.setReference(parent); // creates circular reference

        testObject = parent;
    }

    @When("the object is validated")
    public void theObjectIsValidated() {
        textWorld.setLastException(catchThrowable(() ->
                annotationValidator.validate(testObject)));
    }

    // Test model classes
    @Data
    @Builder
    public static class ValidTestObject {

        @NotBlank
        private final String name;

    }

    @Data
    @Builder
    public static class InvalidTestObject {

        @NotBlank
        private final String name;

    }

    @Data
    @Builder
    public static class ParentTestObject {

        @NotNull
        private final InvalidTestObject child;

    }

    @Getter
    @Setter
    @Builder
    public static class CircularTestObject {

        private final String name;
        private CircularTestObject reference;

    }

}
