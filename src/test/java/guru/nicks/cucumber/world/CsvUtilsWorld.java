package guru.nicks.cucumber.world;

import io.cucumber.spring.ScenarioScope;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.jackson.Jacksonized;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ScenarioScope
@Data
public class CsvUtilsWorld {

    private List<Dto> parsed;

    @Value
    @NonFinal
    @Jacksonized
    @Builder(toBuilder = true)
    public static class Dto {

        String firstName;
        String lastName;
        Integer age;

    }

}
