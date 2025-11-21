package guru.nicks.commons.compressor;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.compressor.GzipCompressorUtils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing {@link GzipCompressorUtilsSteps}.
 */
@RequiredArgsConstructor
public class GzipCompressorUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private byte[] inputData;
    private byte[] compressedData;

    @Given("data to gzip-compress {string}")
    public void dataToCompress(String input) {
        inputData = input.getBytes(StandardCharsets.UTF_8);
    }

    @Given("data to gzip-compress is null")
    public void dataToCompressIsNull() {
        inputData = null;
    }

    @Given("invalid gzip-compressed data")
    public void invalidGzipCompressedData() {
        // generate random bytes that are not valid Gzip compressed data
        var random = new Random();
        inputData = new byte[20];
        random.nextBytes(inputData);
    }

    @When("the data is compressed using Gzip")
    public void theDataIsCompressedUsingGzip() {
        try {
            compressedData = GzipCompressorUtils.compress(inputData);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("the data is decompressed using Gzip")
    public void theDataIsDecompressedUsingGzip() {
        try {
            GzipCompressorUtils.decompress(inputData);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @Then("the gzip-compressed data should not be empty")
    public void theGzipCompressedDataShouldNotBeEmpty() {
        assertThat(compressedData)
                .as("compressedData")
                .isNotEmpty();
    }

    @Then("the gzip-compressed data should be different from original")
    public void theGzipCompressedDataShouldBeDifferentFromOriginal() {
        assertThat(compressedData)
                .as("compressedData")
                .isNotEqualTo(inputData);
    }

    @Then("the gzip-compressed data should be decompressable back to original")
    public void theGzipCompressedDataShouldBeDecompressableBackToOriginal() {
        byte[] decompressed = GzipCompressorUtils.decompress(compressedData);

        assertThat(decompressed)
                .as("decompressedData")
                .isEqualTo(inputData);
    }

}
