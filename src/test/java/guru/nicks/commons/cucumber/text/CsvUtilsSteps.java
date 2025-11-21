package guru.nicks.commons.cucumber.text;

import guru.nicks.commons.cucumber.world.CsvUtilsWorld;
import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.utils.text.CsvUtils;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class CsvUtilsSteps {

    // DI
    private final TextWorld textWorld;
    private final CsvUtilsWorld csvUtilsWorld;

    @When("CSV is parsed")
    @SneakyThrows
    public void csvIsParsed() {
        var iterator = CsvUtils.parseCsv(
                new ByteArrayInputStream(textWorld.getInput().getBytes(StandardCharsets.UTF_8)),
                CsvUtilsWorld.Dto.class);
        csvUtilsWorld.setParsed(iterator.readAll());
    }

    @Then("parsed CSV should be {string}, {string}, {int}, {string}, {string}, {int}")
    public void parsedCsvShouldBe(String firstName1, String lastName1, Integer age1,
            String firstName2, String lastName2, Integer age2) {
        assertThat(csvUtilsWorld.getParsed().getFirst().getFirstName())
                .as("first record firstName")
                .isEqualTo(firstName1);

        assertThat(csvUtilsWorld.getParsed().getFirst().getLastName())
                .as("first record lastName")
                .isEqualTo(lastName1);

        assertThat(csvUtilsWorld.getParsed().getFirst().getAge())
                .as("first record age")
                .isEqualTo(age1);

        assertThat(csvUtilsWorld.getParsed().get(1).getFirstName())
                .as("second record firstName")
                .isEqualTo(firstName2);

        assertThat(csvUtilsWorld.getParsed().get(1).getLastName())
                .as("second record lastName")
                .isEqualTo(lastName2);

        assertThat(csvUtilsWorld.getParsed().get(1).getAge())
                .as("second record age")
                .isEqualTo(age2);
    }
}
