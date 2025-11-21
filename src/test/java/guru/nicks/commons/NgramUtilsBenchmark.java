package guru.nicks.commons;

import guru.nicks.commons.utils.text.NgramUtils;
import guru.nicks.commons.utils.text.NgramUtilsConfig;

import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Benchmark for {@link NgramUtils#createNgrams(String, NgramUtils.Mode, NgramUtilsConfig)}.
 * <p>
 * JMH is configured and run by JMH Maven Plugin; if this class is run from within IDE, slower defaults will apply.
 */
@State(Scope.Benchmark)
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
