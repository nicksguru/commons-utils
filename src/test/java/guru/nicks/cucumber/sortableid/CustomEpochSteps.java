package guru.nicks.cucumber.sortableid;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.sortableid.TimeSortableId;

import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@RequiredArgsConstructor
public class CustomEpochSteps {

    // DI
    private final TextWorld textWorld;

    @When("sequence number is converted to time-sortable")
    public void idIsConvertedToTimeSortable() {
        textWorld.setOutput(new TimeSortableId(textWorld.getDate(),
                // numbers may contain whitespaces, for readability
                Long.parseLong(StringUtils.deleteWhitespace(textWorld.getInput()))
        ).getId());
    }

    @When("time-sortable ID is decoded")
    public void timeSortableIdIsParsed() {
        Optional<TimeSortableId> timeSortableId = TimeSortableId.decode(textWorld.getInput());

        textWorld.setOutput(
                timeSortableId.map(TimeSortableId::getSequence)
                        .map(Object::toString)
                        .orElse(null));

        textWorld.setParsedDate(
                timeSortableId.map(TimeSortableId::getTimestamp).orElse(null));
    }

}
