package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.utils.FutureUtils;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@RequiredArgsConstructor
public class FutureUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private final List<Integer> executionOrder = new CopyOnWriteArrayList<>();
    private final AtomicBoolean mdcCheckPassed = new AtomicBoolean(false);

    private List<Supplier<Integer>> suppliers;
    private List<Runnable> runnables;
    private List<Integer> results;

    private String mdcKey;
    private String mdcValue;

    @After
    public void afterEachScenario() {
        MDC.clear();
    }

    @Given("{int} suppliers that return their index")
    public void suppliersReturningTheirIndex(int count) {
        suppliers = IntStream.range(0, count)
                .mapToObj(i -> (Supplier<Integer>) () -> i)
                .toList();
    }

    @Given("{int} runnables that store their execution order")
    public void runnablesThatStoreExecutionOrder(int count) {
        executionOrder.clear();

        runnables = IntStream.range(0, count)
                .mapToObj(i -> (Runnable) () -> executionOrder.add(i))
                .toList();
    }

    @Given("a supplier that throws an exception")
    public void supplierThatThrowsException() {
        suppliers = new ArrayList<>();

        suppliers.add(() -> {
            throw new RuntimeException("Test exception");
        });
    }

    @Given("a runnable that throws an exception")
    public void runnableThatThrowsException() {
        runnables = new ArrayList<>();

        runnables.add(() -> {
            throw new RuntimeException("Test exception");
        });
    }

    @Given("a supplier that checks MDC context")
    public void supplierThatChecksMdcContext() {
        suppliers = new ArrayList<>();

        suppliers.add(() -> {
            String value = MDC.get(mdcKey);
            mdcCheckPassed.set(mdcValue.equals(value));
            return 1;
        });
    }

    @Given("a runnable that checks MDC context")
    public void runnableThatChecksMdcContext() {
        runnables = new ArrayList<>();

        runnables.add(() -> {
            String value = MDC.get(mdcKey);
            mdcCheckPassed.set(mdcValue.equals(value));
        });
    }

    @Given("MDC context is set with key {string} and value {string}")
    public void mdcContextIsSet(String key, String value) {
        mdcKey = key;
        mdcValue = value;
        MDC.put(key, value);
    }

    @When("the suppliers are executed in parallel with {int} threads")
    public void suppliersAreExecutedInParallel(int threadLimit) {
        Throwable thrown = catchThrowable(() ->
                results = FutureUtils.getInParallel(suppliers, threadLimit));

        textWorld.setLastException(thrown);
    }

    @When("the runnables are executed in parallel with {int} threads")
    public void runnablesAreExecutedInParallel(int threadLimit) {
        Throwable thrown = catchThrowable(() ->
                FutureUtils.runInParallel(runnables, threadLimit));

        textWorld.setLastException(thrown);
    }

    @When("the supplier is executed in parallel")
    public void supplierIsExecutedInParallel() {
        Throwable thrown = catchThrowable(() ->
                results = FutureUtils.getInParallel(suppliers));

        textWorld.setLastException(thrown);
    }

    @When("the runnable is executed in parallel")
    public void runnableIsExecutedInParallel() {
        Throwable thrown = catchThrowable(() ->
                FutureUtils.runInParallel(runnables));

        textWorld.setLastException(thrown);
    }

    @Then("{int} results should be collected in the original order")
    public void resultsShouldBeCollectedInOriginalOrder(int count) {
        assertThat(results)
                .as("results")
                .hasSize(count);

        for (int i = 0; i < count; i++) {
            assertThat(results.get(i))
                    .as("result at index " + i)
                    .isEqualTo(i);
        }
    }

    @Then("all {int} runnables should have been executed")
    public void allRunnablesShouldHaveBeenExecuted(int count) {
        assertThat(executionOrder)
                .as("executionOrder")
                .hasSize(count);

        // Check that all expected indices are present, regardless of order
        List<Integer> expectedIndices = IntStream.range(0, count).boxed().toList();
        assertThat(executionOrder)
                .as("executionOrder")
                .containsExactlyInAnyOrderElementsOf(expectedIndices);
    }

    @Then("the supplier should have access to the MDC context")
    public void supplierShouldHaveAccessToMdcContext() {
        assertThat(mdcCheckPassed.get())
                .as("mdcCheckPassed")
                .isTrue();
    }

    @Then("the runnable should have access to the MDC context")
    public void runnableShouldHaveAccessToMdcContext() {
        assertThat(mdcCheckPassed.get())
                .as("mdcCheckPassed")
                .isTrue();
    }

}
