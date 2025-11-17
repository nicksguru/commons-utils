package guru.nicks.commons.cucumber;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.HashUtils;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@RequiredArgsConstructor
public class HashUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private byte[] inputBytes;
    private byte[] hashResult;
    private HashUtils hashAlgorithm;
    private int hashLength;

    @DataTableType
    public HashTestData createHashTestData(Map<String, String> entry) {
        return HashTestData.builder()
                .input(entry.get("input"))
                .algorithm(HashUtils.valueOf(entry.get("algorithm")))
                // this is NOT the same as 'getOrDefault(value, 0)' which passes nulls as-is
                .length(Optional.ofNullable(entry.get("length"))
                        .map(Integer::parseInt)
                        .orElse(0))
                .expectedOutput(entry.get("expectedOutput"))
                .expectException(Boolean.parseBoolean(entry.getOrDefault("expectException", "false")))
                .build();
    }

    @Given("input string {string}")
    public void givenInputString(String input) {
        inputBytes = input.getBytes(StandardCharsets.UTF_8);
    }

    @Given("hash algorithm {string}")
    public void givenHashAlgorithm(String algorithm) {
        hashAlgorithm = HashUtils.valueOf(algorithm);
    }

    @Given("hash length {int}")
    public void givenHashLength(int length) {
        hashLength = length;
    }

    @When("the hash is computed")
    public void whenTheHashIsComputed() {
        var throwable = catchThrowable(() ->
                hashResult = hashAlgorithm.compute(inputBytes));

        textWorld.setLastException(throwable);
    }

    @When("the hash is computed with specified length")
    public void whenTheHashIsComputedWithSpecifiedLength() {
        var throwable = catchThrowable(() ->
                hashResult = hashAlgorithm.compute(inputBytes, hashLength));

        textWorld.setLastException(throwable);
    }

    @Then("the hash result should have length {int}")
    public void thenTheHashResultShouldHaveLength(int expectedLength) {
        assertThat(hashResult)
                .as("hashResult")
                .hasSize(expectedLength);
    }

    @Then("the hash result should match {string}")
    public void thenTheHashResultShouldMatch(String expectedHexString) {
        byte[] expected = hexStringToByteArray(expectedHexString);
        assertThat(hashResult)
                .as("hashResult")
                .isEqualTo(expected);
    }

    @Then("the hash result as string should be {string}")
    public void thenTheHashResultAsStringShouldBe(String expectedString) {
        String resultString = new String(hashResult, StandardCharsets.UTF_8);

        assertThat(resultString)
                .as("hashResult as string")
                .isEqualTo(expectedString);
    }

    @When("the following hash operations are performed:")
    public void whenTheFollowingHashOperationsArePerformed(DataTable dataTable) {
        for (HashTestData testData : dataTable.entries().stream()
                .map(this::createHashTestData)
                .toList()) {
            inputBytes = testData.getInput().getBytes(StandardCharsets.UTF_8);
            hashAlgorithm = testData.getAlgorithm();
            hashLength = testData.getLength();

            var throwable = catchThrowable(() -> {
                // 0 means use default (for the given algorithm) hash length
                hashResult = (hashLength > 0)
                        ? hashAlgorithm.compute(inputBytes, hashLength)
                        : hashAlgorithm.compute(inputBytes);

                if (testData.isExpectException()) {
                    assertThat(false)
                            .as("Expected exception but none was thrown")
                            .isTrue();
                }

                if (testData.getExpectedOutput() != null) {
                    // plain strings because these algorithms generate decimal digits
                    if ((testData.getAlgorithm() == HashUtils.LUHN_DIGIT)
                            || (testData.getAlgorithm() == HashUtils.ISIN_DIGIT)
                            || (testData.getAlgorithm() == HashUtils.VERHOEFF)) {
                        String resultString = new String(hashResult, StandardCharsets.UTF_8);
                        assertThat(resultString)
                                .as("hashResult as string")
                                .isEqualTo(testData.getExpectedOutput());
                    }
                    // hex strings because other algorithms generate binary output
                    else {
                        byte[] expected = hexStringToByteArray(testData.getExpectedOutput());
                        assertThat(hashResult)
                                .as("hashResult")
                                .isEqualTo(expected);
                    }
                }
            });

            textWorld.setLastException(throwable);

            if (throwable != null && !testData.isExpectException()) {
                throw new AssertionError("Unexpected exception: " + throwable.getMessage(), throwable);
            }
        }
    }

    @Then("the max hash length for {string} should be {int}")
    public void thenTheMaxHashLengthForShouldBe(String algorithm, int expectedLength) {
        HashUtils hashAlg = HashUtils.valueOf(algorithm);

        assertThat(hashAlg.getMaxHashLengthBytes())
                .as("getMaxHashLengthBytes()")
                .isEqualTo(expectedLength);
    }

    @Then("the default hash length for {string} should be {int}")
    public void thenTheDefaultHashLengthForShouldBe(String algorithm, int expectedLength) {
        HashUtils hashAlg = HashUtils.valueOf(algorithm);

        assertThat(hashAlg.getDefaultHashLengthBytes())
                .as("getDefaultHashLengthBytes()")
                .isEqualTo(expectedLength);
    }

    // Helper method to convert hex string to byte array
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @Value
    @Builder
    public static class HashTestData {

        String input;
        HashUtils algorithm;
        int length;

        String expectedOutput;
        boolean expectException;

    }

}
