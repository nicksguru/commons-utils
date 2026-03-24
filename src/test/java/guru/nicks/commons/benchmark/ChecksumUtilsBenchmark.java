package guru.nicks.commons.benchmark;

import guru.nicks.commons.utils.crypto.ChecksumUtils;

import lombok.Getter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static guru.nicks.commons.validation.dsl.ValiDsl.checkNotNull;

/**
 * Benchmark for {@link ChecksumUtils#computeJsonChecksum(Object)}.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@Threads(1)
public class ChecksumUtilsBenchmark {

    private TestObject smallObject;
    private TestObject mediumObject;
    private TestObject largeObject;

    /**
     * Creates a small test object with minimal data.
     */
    private static TestObject createSmallObject() {
        return new TestObject("test", 42);
    }

    /**
     * Creates a medium-sized test object with nested structure.
     */
    private static TestObject createMediumObject() {
        var obj = new TestObject("medium-object", 100);
        obj.addProperty("key1", "value1");
        obj.addProperty("key2", "value2");
        obj.addProperty("key3", "value3");
        return obj;
    }

    /**
     * Creates a large test object with extensive nested data.
     */
    private static TestObject createLargeObject() {
        var obj = new TestObject("large-object", 1000);

        for (int i = 0; i < 100; i++) {
            obj.addProperty("key" + i, "value" + i + "-".repeat(10));
        }

        return obj;
    }

    @Setup
    public void setup() {
        smallObject = createSmallObject();
        mediumObject = createMediumObject();
        largeObject = createLargeObject();
    }

    @Benchmark
    public String computeJsonChecksum_SmallInput() {
        return checkNotNull(ChecksumUtils.computeJsonChecksum(smallObject), "checksum");
    }

    @Benchmark
    public String computeJsonChecksum_MediumInput() {
        return checkNotNull(ChecksumUtils.computeJsonChecksum(mediumObject), "checksum");
    }

    @Benchmark
    public String computeJsonChecksum_LargeInput() {
        return checkNotNull(ChecksumUtils.computeJsonChecksum(largeObject), "checksum");
    }

    /**
     * Test object class for benchmarking. Contains string, integer, and nested map data.
     */
    @Getter
    private static class TestObject {

        private final String name;
        private final int value;
        private final Map<String, Object> properties;

        TestObject(String name, int value) {
            this.name = name;
            this.value = value;
            this.properties = new LinkedHashMap<>();
        }

        void addProperty(String key, Object value) {
            properties.put(key, value);
        }

    }

}
