package guru.nicks.commons.cucumber.world;

import io.cucumber.spring.ScenarioScope;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ScenarioScope
@Data
public class CsvUtilsWorld {

    private List<Dto> parsed;

    @Builder(toBuilder = true)
    public record Dto(

            String firstName,
            String lastName,
            Integer age) {
    }

}
