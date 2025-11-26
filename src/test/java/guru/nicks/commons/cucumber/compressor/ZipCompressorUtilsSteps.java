package guru.nicks.commons.cucumber.compressor;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.compressor.ZipCompressorUtils;
import guru.nicks.commons.utils.compressor.ZstdCompressorUtils;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing {@link ZstdCompressorUtils}.
 */
@RequiredArgsConstructor
public class ZipCompressorUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private byte[] inputData;
    private byte[] compressedData;

    @Given("data to zip-compress {string}")
    public void dataToZipCompress(String input) {
        inputData = input.getBytes(StandardCharsets.UTF_8);
    }

    @Given("data to zip-compress is null")
    public void dataToZipCompressIsNull() {
        inputData = null;
    }

    @Given("invalid zip-compressed data")
    public void invalidZipCompressedData() {
        // generate random bytes that are not valid Zip compressed data
        var random = new Random();
        inputData = new byte[20];
        random.nextBytes(inputData);
    }

    @When("the data is compressed using Zip")
    public void theDataIsCompressedUsingZip() {
        try {
            compressedData = ZipCompressorUtils.compress(inputData);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("the data is decompressed using Zip")
    public void theDataIsDecompressedUsingZip() {
        try {
            ZipCompressorUtils.decompress(inputData);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @Then("the zip-compressed data should not be empty")
    public void theZipCompressedDataShouldNotBeEmpty() {
        assertThat(compressedData)
                .as("compressedData")
                .isNotEmpty();
    }

    @Then("the zip-compressed data should be different from original")
    public void theZipCompressedDataShouldBeDifferentFromOriginal() {
        assertThat(compressedData)
                .as("compressedData")
                .isNotEqualTo(inputData);
    }

    @Then("the zip-compressed data should be decompressable back to original")
    public void theZipCompressedDataShouldBeDecompressableBackToOriginal() {
        byte[] decompressed = ZipCompressorUtils.decompress(compressedData);

        assertThat(decompressed)
                .as("decompressedData")
                .isEqualTo(inputData);
    }

}
