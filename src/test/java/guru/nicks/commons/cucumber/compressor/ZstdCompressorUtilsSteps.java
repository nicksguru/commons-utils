package guru.nicks.commons.cucumber.compressor;

import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.compressor.ZstdCompressorUtils;

import com.github.luben.zstd.ZstdCompressCtx;
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
public class ZstdCompressorUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private byte[] inputData;
    private byte[] compressedData;

    @Given("data to zstd-compress {string}")
    public void dataToCompress(String input) {
        inputData = input.getBytes(StandardCharsets.UTF_8);
    }

    @Given("data to zstd-compress is null")
    public void dataToCompressIsNull() {
        inputData = null;
    }

    @Given("invalid zstd-compressed data")
    public void invalidZstdCompressedData() {
        // generate random bytes that are not valid Zstd compressed data
        var random = new Random();
        inputData = new byte[20];
        random.nextBytes(inputData);
    }

    @Given("zstd-compressed data without content size")
    public void zstdCompressedDataWithoutContentSize() {
        // streaming-style frame whose header declares no content size
        try (ZstdCompressCtx ctx = new ZstdCompressCtx()) {
            ctx.setContentSize(false);
            inputData = ctx.compress("data without content size".getBytes(StandardCharsets.UTF_8));
        }
    }

    @Given("zstd-compressed data with decompressed size exceeding the limit")
    public void zstdCompressedDataWithDecompressedSizeExceedingTheLimit() {
        // 1 extra byte of zeros compresses into a tiny frame that still declares the oversized content size
        inputData = ZstdCompressorUtils.compress(new byte[ZstdCompressorUtils.MAX_DECOMPRESSED_SIZE + 1]);
    }

    @When("the data is compressed using Zstd")
    public void theDataIsCompressedUsingZstd() {
        try {
            compressedData = ZstdCompressorUtils.compress(inputData);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("the data is decompressed using Zstd")
    public void theDataIsDecompressedUsingZstd() {
        try {
            ZstdCompressorUtils.decompress(inputData);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @Then("the zstd-compressed data should not be empty")
    public void theZstdCompressedDataShouldNotBeEmpty() {
        assertThat(compressedData)
                .as("compressedData")
                .isNotEmpty();
    }

    @Then("the zstd-compressed data should be different from original")
    public void theZstdCompressedDataShouldBeDifferentFromOriginal() {
        assertThat(compressedData)
                .as("compressedData")
                .isNotEqualTo(inputData);
    }

    @Then("the zstd-compressed data should be decompressable back to original")
    public void theZstdCompressedDataShouldBeDecompressableBackToOriginal() {
        byte[] decompressed = ZstdCompressorUtils.decompress(compressedData);

        assertThat(decompressed)
                .as("decompressedData")
                .isEqualTo(inputData);
    }

}
