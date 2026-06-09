package guru.nicks.commons.benchmark;

import guru.nicks.commons.utils.text.NgramUtils;
import guru.nicks.commons.utils.text.NgramUtilsConfig;

import org.apache.commons.lang3.RandomStringUtils;
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

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Benchmark for {@link NgramUtils}.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@Threads(1)
public class NgramUtilsBenchmark {

    public static final int WORD_COUNT = 1_000;
    public static final int MIN_WORD_LENGTH = 1;
    public static final int MAX_WORD_LENGTH = 15;

    private String text;

    @Setup
    public void setup() {
        text = IntStream.range(0, WORD_COUNT)
                .mapToObj(i -> RandomStringUtils.insecure().nextAlphabetic(MIN_WORD_LENGTH, MAX_WORD_LENGTH))
                .collect(Collectors.joining(" "));
    }

    @Benchmark
    public void createNgrams() {
        NgramUtils.createNgrams(text, NgramUtils.Mode.ALL, NgramUtilsConfig.DEFAULT);
    }

}
