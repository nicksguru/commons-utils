package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.utils.JvmUtils;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@RequiredArgsConstructor
public class JvmUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private AutoCloseable closeableMocks;
    private MockedStatic<JvmUtils.DirectMemoryAccessor> directMemoryAccessorMockedStatic;
    private DataSize retrievedMemory;
    private long retrievedBytes;
    private int memoryAccessorCallCount;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        directMemoryAccessorMockedStatic = mockStatic(JvmUtils.DirectMemoryAccessor.class);
        memoryAccessorCallCount = 0;
        JvmUtils.invalidateCache();
    }

    @After
    public void afterEachScenario() throws Exception {
        if (directMemoryAccessorMockedStatic != null) {
            directMemoryAccessorMockedStatic.close();
        }
        closeableMocks.close();
    }

    @Given("JVM max memory is set to {long} bytes")
    public void jvmMaxMemoryIsSetToBytes(long bytes) {
        directMemoryAccessorMockedStatic.when(JvmUtils.DirectMemoryAccessor::getMaxMemoryBytes)
                .thenAnswer(invocation -> {
                    memoryAccessorCallCount++;
                    return bytes;
                });
    }

    @Given("JVM free memory is set to {long} bytes")
    public void jvmFreeMemoryIsSetToBytes(long bytes) {
        directMemoryAccessorMockedStatic.when(JvmUtils.DirectMemoryAccessor::getFreeMemoryBytes)
                .thenAnswer(invocation -> {
                    memoryAccessorCallCount++;
                    return bytes;
                });
    }

    @Given("JVM total memory is set to {long} bytes")
    public void jvmTotalMemoryIsSetToBytes(long bytes) {
        directMemoryAccessorMockedStatic.when(JvmUtils.DirectMemoryAccessor::getTotalMemoryBytes)
                .thenAnswer(invocation -> {
                    memoryAccessorCallCount++;
                    return bytes;
                });

        // also set max memory to simulate Long.MAX_VALUE scenario
        directMemoryAccessorMockedStatic.when(JvmUtils.DirectMemoryAccessor::getMaxMemoryBytes)
                .thenReturn(Long.MAX_VALUE);
    }

    @When("max memory is requested")
    public void maxMemoryIsRequested() {
        retrievedMemory = JvmUtils.getMaxMemory();
    }

    @When("free memory is requested")
    public void freeMemoryIsRequested() {
        retrievedMemory = JvmUtils.getFreeMemory();
    }

    @When("max memory is requested {int} times")
    public void maxMemoryIsRequestedTimes(int times) {
        for (int i = 0; i < times; i++) {
            retrievedMemory = JvmUtils.getMaxMemory();
        }
    }

    @When("{word} memory bytes are accessed directly")
    public void memoryBytesAreAccessedDirectly(String memoryType) {
        switch (memoryType) {
            case "total" -> retrievedBytes = JvmUtils.DirectMemoryAccessor.getTotalMemoryBytes();
            case "max" -> retrievedBytes = JvmUtils.DirectMemoryAccessor.getMaxMemoryBytes();
            case "free" -> retrievedBytes = JvmUtils.DirectMemoryAccessor.getFreeMemoryBytes();
            default -> throw new IllegalArgumentException("Invalid memory type: '" + memoryType + "'");
        }
    }

    @Then("the returned memory size should be {long} bytes")
    public void theReturnedMemorySizeShouldBeBytes(long expectedBytes) {
        assertThat(retrievedMemory.toBytes())
                .as("retrieved memory size in bytes")
                .isEqualTo(expectedBytes);
    }

    @Then("the returned value should be {long} bytes")
    public void theReturnedValueShouldBeBytes(long expectedBytes) {
        assertThat(retrievedBytes)
                .as("retrieved bytes value")
                .isEqualTo(expectedBytes);
    }

    @Then("runtime should be accessed only once for max memory")
    public void runtimeShouldBeAccessedOnlyOnceForMaxMemory() {
        directMemoryAccessorMockedStatic.verify(JvmUtils.DirectMemoryAccessor::getMaxMemoryBytes);

        assertThat(memoryAccessorCallCount)
                .as("memory accessor call count")
                .isEqualTo(1);
    }

}
