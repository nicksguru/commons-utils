package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.ReflectionUtils;
import guru.nicks.commons.utils.TransformUtils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class ReflectionUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private Object testInput;
    private boolean result;

    @Given("class name is {string}")
    public void classNameIs(String className) {
        textWorld.setInput(className);
    }

    @When("class hierarchy is discovered")
    public void classHierarchyIsDiscovered() throws ClassNotFoundException {
        Set<Class<?>> hierarchy = ReflectionUtils.getClassHierarchy(
                getClass().getClassLoader().loadClass(textWorld.getInput()));
        textWorld.setOutput(TransformUtils.toList(hierarchy, Class::getName));
    }

    @Then("class hierarchy should contain {string} at index {int}")
    public void classHierarchyShouldContainAtIndex(String className, int index) {
        assertThat(textWorld.getOutput().get(index))
                .as("Class name at index %d", index)
                .isEqualTo(className);
    }

    @Then("class hierarchy length should be {int}")
    public void classHierarchyLengthShouldBe(int length) {
        assertThat(textWorld.getOutput())
                .as("Class hierarchy size")
                .hasSize(length);
    }

    @When("the isScalar method is called with {string}")
    public void theIsScalarMethodIsCalledWithString(String input) {
        testInput = input;
        result = ReflectionUtils.isScalar(testInput);
    }

    @When("the isScalar method is called with {double}")
    public void theIsScalarMethodIsCalledWithDouble(double input) {
        testInput = input;
        result = ReflectionUtils.isScalar(testInput);
    }

    @When("the isScalar method is called with {booleanValue}")
    public void theIsScalarMethodIsCalledWithBoolean(boolean input) {
        testInput = input;
        result = ReflectionUtils.isScalar(testInput);
    }

    @When("the isScalar method is called with null")
    public void theIsScalarMethodIsCalledWithNull() {
        testInput = null;
        result = ReflectionUtils.isScalar(testInput);
    }

    @When("the isScalar method is called with a list")
    public void theIsScalarMethodIsCalledWithAList() {
        testInput = new ArrayList<String>();
        result = ReflectionUtils.isScalar(testInput);
    }

    @When("the isScalar method is called with a array")
    public void theIsScalarMethodIsCalledWithAnArray() {
        testInput = new String[0];
        result = ReflectionUtils.isScalar(testInput);
    }

    @When("the isScalar method is called with a map")
    public void theIsScalarMethodIsCalledWithAMap() {
        testInput = new HashMap<String, Object>();
        result = ReflectionUtils.isScalar(testInput);
    }

    @When("the isScalar method is called with a custom object")
    public void theIsScalarMethodIsCalledWithACustomObject() {
        testInput = new CustomTestObject();
        result = ReflectionUtils.isScalar(testInput);
    }

    @When("the isScalar method is called with a collection")
    public void theIsScalarMethodIsCalledWithACollection() {
        testInput = new HashSet<String>();
        result = ReflectionUtils.isScalar(testInput);
    }

    @Then("the result should be {booleanValue}")
    public void theResultShouldBe(boolean expected) {
        assertThat(result)
                .as("isScalar result")
                .isEqualTo(expected);
    }

    /**
     * Simple custom class for testing purposes.
     */
    private static class CustomTestObject {

        @Override
        public String toString() {
            String name = "test";
            return "CustomTestObject{name='" + name + "'}";
        }

    }

}
