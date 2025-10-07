package guru.nicks.cucumber;

import guru.nicks.utils.TransformUtils;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing {@link TransformUtils} functionality.
 */
public class TransformUtilsSteps {

    private final List<String> stringList = new ArrayList<>();
    private final List<String> stringifiedResults = new ArrayList<>();
    private Stream<Object> inputStream;

    private List<?> result;
    private Map<Object, ? extends List<?>> groupedResult;
    private List<String> resultList;
    private Set<String> resultSet;

    private List<TestObject> testObjects = new ArrayList<>();
    private ComplexObject complexObject;
    private String stringifiedComplexObject;

    @DataTableType
    public TestObject createTestObject(Map<String, String> entry) {
        return TestObject.builder()
                .type(entry.get("type"))
                .value(entry.get("value"))
                .build();
    }

    @DataTableType
    public StringifyResult createStringifyResult(Map<String, String> entry) {
        return StringifyResult.builder()
                .type(entry.get("type"))
                .result(entry.get("result"))
                .build();
    }

    @Given("a list of strings {string}")
    public void aListOfStrings(String commaSeparatedStrings) {
        stringList.clear();
        stringList.addAll(Arrays.asList(commaSeparatedStrings.split(",")));
    }

    @Given("the following objects:")
    public void theFollowingObjects(List<TestObject> objects) {
        testObjects = objects;
    }

    @Given("a complex object")
    public void aComplexObject() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "Test Object");
        properties.put("value", 123);
        properties.put("active", true);

        complexObject = ComplexObject.builder()
                .name("Test Object")
                .value(123)
                .active(true)
                .items(Arrays.asList("item1", "item2", "item3"))
                .properties(properties)
                .build();
    }

    @When("the list is transformed to uppercase using a single mapper")
    public void theListIsTransformedToUppercaseUsingASingleMapper() {
        resultList = TransformUtils.toList(stringList, String::toUpperCase);
    }

    @When("the list is transformed to uppercase and then reversed using two mappers")
    public void theListIsTransformedToUppercaseAndThenReversedUsingTwoMappers() {
        Function<String, String> reverseString = s -> new StringBuilder(s).reverse().toString();
        resultList = TransformUtils.toList(stringList, String::toUpperCase, reverseString);
    }

    @When("the list is transformed to uppercase, reversed, and then length calculated using three mappers")
    public void theListIsTransformedToUppercaseReversedAndThenLengthCalculatedUsingThreeMappers() {
        Function<String, String> reverseString = s -> new StringBuilder(s).reverse().toString();

        resultList = TransformUtils.toList(stringList, String::toUpperCase, reverseString, String::length)
                .stream()
                .map(Object::toString)
                .toList();
    }

    @When("the list is transformed to a set of uppercase strings using a single mapper")
    public void theListIsTransformedToASetOfUppercaseStringsUsingASingleMapper() {
        resultSet = TransformUtils.toSet(stringList, String::toUpperCase);
    }

    @When("the list is transformed to a set using uppercase and then first letter extraction with two mappers")
    public void theListIsTransformedToASetUsingUppercaseAndThenFirstLetterExtractionWithTwoMappers() {
        Function<String, String> firstLetter = s -> s.substring(0, 1);
        resultSet = TransformUtils.toSet(stringList, String::toUpperCase, firstLetter);
    }

    @When("the list is transformed to a set using uppercase, first letter extraction, and ASCII code with three mappers")
    public void theListIsTransformedToASetUsingUppercaseFirstLetterExtractionAndAsciiCodeWithThreeMappers() {
        Function<String, String> firstLetter = s -> s.substring(0, 1);
        Function<String, Integer> asciiCode = s -> (int) s.charAt(0);

        resultSet = TransformUtils.toSet(stringList, String::toUpperCase, firstLetter, asciiCode)
                .stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }

    @When("a null collection is transformed to a list")
    public void aNullCollectionIsTransformedToAList() {
        resultList = TransformUtils.toList(null, Object::toString);
    }

    @When("a null collection is transformed to a set")
    public void aNullCollectionIsTransformedToASet() {
        resultSet = TransformUtils.toSet(null, Object::toString);
    }

    @When("the objects are stringified")
    public void theObjectsAreStringified() {
        stringifiedResults.clear();

        for (TestObject obj : testObjects) {
            Object value = null;

            if ("string".equals(obj.getType())) {
                value = obj.getValue();
            } else if ("int".equals(obj.getType())) {
                value = Integer.parseInt(obj.getValue());
            }
            // null type remains null

            stringifiedResults.add(TransformUtils.stringify(value));
        }
    }

    @When("the complex object is stringified")
    public void theComplexObjectIsStringified() {
        stringifiedComplexObject = TransformUtils.stringify(complexObject);
    }

    @When("the complex object is stringified with pretty print")
    public void theComplexObjectIsStringifiedWithPrettyPrint() {
        stringifiedComplexObject = TransformUtils.stringify(complexObject, true);
    }

    @Then("the result should be a list containing {string}")
    public void theResultShouldBeAListContaining(String commaSeparatedExpectedValues) {
        List<String> expectedValues = Arrays.asList(commaSeparatedExpectedValues.split(","));
        assertThat(resultList)
                .as("resultList")
                .containsExactlyElementsOf(expectedValues);
    }

    @Then("the result should be a set containing {string}")
    public void theResultShouldBeASetContaining(String commaSeparatedExpectedValues) {
        Set<String> expectedValues = Set.of(commaSeparatedExpectedValues.split(","));
        assertThat(resultSet)
                .as("resultSet")
                .containsExactlyInAnyOrderElementsOf(expectedValues);
    }

    @Then("the result should be an empty list")
    public void theResultShouldBeAnEmptyList() {
        assertThat(resultList)
                .as("resultList")
                .isEmpty();
    }

    @Then("the result should be an empty set")
    public void theResultShouldBeAnEmptySet() {
        assertThat(resultSet)
                .as("resultSet")
                .isEmpty();
    }

    @Then("the stringified results should be:")
    public void theStringifiedResultsShouldBe(List<StringifyResult> expectedResults) {
        List<String> expected = expectedResults.stream()
                .map(StringifyResult::getResult)
                // empty strings can't be passed, rather represented in some form - '' in this case
                .map(str -> "''".equals(str) ? "" : str)
                .toList();

        assertThat(stringifiedResults)
                .as("stringifiedResults")
                .containsExactlyElementsOf(expected);
    }

    @Then("the result should be a JSON string")
    public void theResultShouldBeAJsonString() {
        assertThat(stringifiedComplexObject)
                .as("stringifiedComplexObject")
                .startsWith("{");
        assertThat(stringifiedComplexObject)
                .as("stringifiedComplexObject")
                .endsWith("}");
    }

    @Then("the JSON string should contain the object's properties")
    public void theJsonStringShouldContainTheObjectSProperties() {
        assertThat(stringifiedComplexObject)
                .as("stringifiedComplexObject")
                .contains("\"name\"");
        assertThat(stringifiedComplexObject)
                .as("stringifiedComplexObject")
                .contains("\"value\"");
        assertThat(stringifiedComplexObject)
                .as("stringifiedComplexObject")
                .contains("\"active\"");
        assertThat(stringifiedComplexObject)
                .as("stringifiedComplexObject")
                .contains("\"items\"");
        assertThat(stringifiedComplexObject)
                .as("stringifiedComplexObject")
                .contains("\"properties\"");
    }

    @Then("the JSON string should contain line breaks")
    public void theJsonStringShouldContainLineBreaks() {
        assertThat(stringifiedComplexObject)
                .as("stringifiedComplexObject")
                .contains("\n");
    }

    @Given("stream elements {string}")
    public void streamElements(String commaSeparatedString) {
        if (commaSeparatedString.contains(",")) {
            inputStream = Arrays.stream(commaSeparatedString.split(","));
        } else {
            inputStream = Stream.of(commaSeparatedString);
        }
    }

    @Given("a null stream")
    public void nullStream() {
        inputStream = null;
    }

    @When("the stream is transformed with mapping function {string}")
    public void streamIsTransformedWithMappingFunction(String functionName) {
        Function<Object, Object> mappingFunction = getMappingFunction(functionName);

        result = inputStream
                .map(mappingFunction)
                .toList();
    }

    @When("the stream is filtered with predicate {string}")
    public void streamIsFilteredWithPredicate(String predicateName) {
        Predicate<Object> filterPredicate = getFilterPredicate(predicateName);

        result = inputStream
                .filter(filterPredicate)
                .toList();
    }

    @When("the stream is grouped by {string}")
    public void streamIsGroupedBy(String groupingFunctionName) {
        Function<Object, Object> keyFunction = getGroupingFunction(groupingFunctionName);
        groupedResult = inputStream.collect(Collectors.groupingBy(keyFunction));
    }

    @Then("the transformed stream should contain {string}")
    public void transformedStreamShouldContain(String expectedString) {
        List<String> expected = expectedString.isEmpty()
                ? Collections.emptyList()
                : Arrays.asList(expectedString.split(","));

        assertThat(result)
                .as("transformed result")
                .hasSize(expected.size());

        for (int i = 0; i < expected.size(); i++) {
            assertThat(result.get(i))
                    .as("element at index " + i)
                    .hasToString(expected.get(i));
        }
    }

    @Then("the grouped result should match {string}")
    public void groupedResultShouldMatch(String expectedString) {
        Map<String, List<String>> expected = parseGroupedExpectedString(expectedString);

        assertThat(groupedResult)
                .as("grouped result")
                .hasSize(expected.size());

        groupedResult.forEach((key, values) -> {
            String keyStr = key.toString();
            List<String> expectedValues = expected.get(keyStr);

            assertThat(expectedValues)
                    .as("values for key " + keyStr)
                    .isNotNull();

            assertThat(values)
                    .as("values list size for key " + keyStr)
                    .hasSize(expectedValues.size());

            for (int i = 0; i < expectedValues.size(); i++) {
                assertThat(values.get(i))
                        .as("value at index " + i + " for key " + keyStr)
                        .hasToString(expectedValues.get(i));
            }
        });
    }

    /**
     * Parses a string representation of grouped expected result. Format: {@code key1:[val1,val2],key2:[val3,val4]}.
     */
    private Map<String, List<String>> parseGroupedExpectedString(String groupedString) {
        Map<String, List<String>> result1 = new HashMap<>();
        String[] groups = groupedString.split(",(?![^\\[]*\\])");

        for (String group : groups) {
            String[] keyValue = group.split(":\\[", 2);
            String key = keyValue[0];
            String valuesStr = keyValue[1].substring(0, keyValue[1].length() - 1);

            List<String> values = valuesStr.isEmpty()
                    ? Collections.emptyList()
                    : Arrays.asList(valuesStr.split(","));

            result1.put(key, values);
        }

        return result1;
    }

    /**
     * Returns the appropriate mapping function based on the name.
     */
    private Function<Object, Object> getMappingFunction(String functionName) {
        return switch (functionName) {
            case "multiply2" -> obj -> Integer.parseInt(obj.toString()) * 2;
            case "uppercase" -> obj -> obj.toString().toUpperCase();
            case "divide2" -> obj -> Integer.parseInt(obj.toString()) / 2;
            case "addPrefix" -> obj -> "prefix_" + obj.toString();
            default -> obj -> obj;
        };
    }

    /**
     * Returns the appropriate filter predicate based on the name.
     */
    private Predicate<Object> getFilterPredicate(String predicateName) {
        return switch (predicateName) {
            case "even" -> obj -> Integer.parseInt(obj.toString()) % 2 == 0;
            case "odd" -> obj -> Integer.parseInt(obj.toString()) % 2 != 0;
            case "startsWithA" -> obj -> obj.toString().startsWith("a");
            default -> obj -> true;
        };
    }

    /**
     * Returns the appropriate grouping function based on the name.
     */
    private Function<Object, Object> getGroupingFunction(String groupingFunctionName) {
        return switch (groupingFunctionName) {
            case "firstLetter" -> obj -> obj.toString().substring(0, 1);
            case "evenOdd" -> obj -> Integer.parseInt(obj.toString()) % 2 == 0 ? "even" : "odd";
            case "length" -> obj -> obj.toString().length();
            default -> obj -> "default";
        };
    }

    /**
     * Data class for test objects.
     */
    @Value
    @Builder
    public static class TestObject {

        String type;
        String value;

    }

    /**
     * Data class for stringify results.
     */
    @Value
    @Builder
    public static class StringifyResult {

        String type;
        String result;

    }

    /**
     * Complex object for testing stringify functionality.
     */
    @Value
    @Builder
    public static class ComplexObject {

        String name;
        int value;
        boolean active;
        List<String> items;
        Map<String, Object> properties;

    }

}
