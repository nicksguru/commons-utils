package guru.nicks.cucumber;

import guru.nicks.cucumber.world.TextWorld;
import guru.nicks.utils.UuidUtils;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for testing {@link UuidUtils} functionality.
 */
@RequiredArgsConstructor
public class UuidUtilsSteps {

    // DI
    private final TextWorld textWorld;

    private final List<UUID> uuids = new ArrayList<>();
    private final List<String> encodedUuids = new ArrayList<>();

    private UUID uuid;
    private String uuidString;
    private String encodedUuid;

    @DataTableType
    public UuidData createUuidData(Map<String, String> entry) {
        return UuidData.builder()
                .uuid(entry.get("uuid"))
                .version(entry.get("version"))
                .build();
    }

    @Given("a UUID {string}")
    public void aUUID(String uuidString) {
        this.uuid = UUID.fromString(uuidString);
    }

    @Given("a UUID string {string}")
    public void aUUIDString(String uuidString) {
        this.uuidString = uuidString;
    }

    @Given("an invalid UUID string {string}")
    public void anInvalidUUIDString(String invalidUuidString) {
        uuidString = invalidUuidString;
    }

    @When("a UUIDv7 is generated")
    public void aUUIDv7IsGenerated() {
        uuid = UuidUtils.generateUuidV7();
    }

    @When("a UUIDv4 is generated")
    public void aUUIDv4IsGenerated() {
        uuid = UuidUtils.generateUuidV4();
    }

    @When("the UUID is encoded to Crockford Base32")
    public void theUUIDIsEncodedToCrockfordBase32() {
        encodedUuid = UuidUtils.encodeToCrockfordBase32(uuid);
    }

    @When("multiple UUIDv7 are generated with delays")
    public void multipleUUIDv7AreGeneratedWithDelays() throws InterruptedException {
        uuids.clear();

        // generate UUIDs with small delays to ensure different timestamps
        for (int i = 0; i < 5; i++) {
            uuids.add(UuidUtils.generateUuidV7());
            // small delay to ensure different timestamps
            Thread.sleep(5);
        }
    }

    @When("the UUID string is parsed")
    public void theUUIDStringIsParsed() {
        uuid = UuidUtils.parse(uuidString);
    }

    @When("the invalid UUID string is parsed")
    public void theInvalidUUIDStringIsParsed() {
        try {
            uuid = UuidUtils.parse(uuidString);
        } catch (Exception e) {
            textWorld.setLastException(e);
        }
    }

    @When("multiple UUIDv7 are generated and encoded to Crockford Base32")
    public void multipleUUIDv7AreGeneratedAndEncodedToCrockfordBase32() throws InterruptedException {
        uuids.clear();
        encodedUuids.clear();

        // generate UUIDs with small delays to ensure different timestamps
        for (int i = 0; i < 5; i++) {
            UUID generatedUuid = UuidUtils.generateUuidV7();
            uuids.add(generatedUuid);
            encodedUuids.add(UuidUtils.encodeToCrockfordBase32(generatedUuid));
            // small delay to ensure different timestamps
            Thread.sleep(5);
        }
    }

    @Then("the UUID should be valid")
    public void theUUIDShouldBeValid() {
        assertThat(uuid)
                .as("uuid")
                .isNotNull();
    }

    @Then("the UUID should be of version {int}")
    public void theUUIDShouldBeOfVersion(int version) {
        assertThat(uuid.version())
                .as("uuid.version()")
                .isEqualTo(version);
    }

    @Then("the encoded string should be {int} characters long")
    public void theEncodedStringShouldBeCharactersLong(int length) {
        assertThat(encodedUuid)
                .as("encodedUuid")
                .hasSize(length);
    }

    @Then("the encoded string should equal {string}")
    public void theEncodedStringShouldEqual(String str) {
        assertThat(encodedUuid)
                .as("encodedUuid")
                .isEqualTo(str);
    }

    @Then("the encoded string should contain only valid Crockford Base32 characters")
    public void theEncodedStringShouldContainOnlyValidCrockfordBase32Characters() {
        // Crockford Base32 alphabet: 0123456789ABCDEFGHJKMNPQRSTVWXYZ (case insensitive)
        // Excluding: I, L, O, U
        Predicate<String> crockfordPattern = Pattern
                .compile("^[0-9A-HJ-NP-TV-Z]+$", Pattern.CASE_INSENSITIVE)
                .asPredicate();

        assertThat(crockfordPattern.test(encodedUuid))
                .as("encodedUuid matches Crockford pattern")
                .isTrue();
    }

    @Then("the UUIDs should be in ascending order when sorted lexicographically")
    public void theUUIDsShouldBeInAscendingOrderWhenSortedLexicographically() {
        List<String> uuidStrings = uuids.stream()
                .map(UUID::toString)
                .toList();

        List<String> sortedUuidStrings = uuidStrings.stream()
                .sorted()
                .toList();

        assertThat(uuidStrings)
                .as("uuidStrings")
                .isEqualTo(sortedUuidStrings);
    }

    @Then("the parsed UUID should equal the original UUID string")
    public void theParsedUUIDShouldEqualTheOriginalUUIDString() {
        assertThat(uuid.toString())
                .as("uuid.toString()")
                .isEqualToIgnoringCase(uuidString);
    }

    @Then("the encoded strings should be in the same order as the original UUIDs")
    public void theEncodedStringsShouldBeInTheSameOrderAsTheOriginalUUIDs() {
        List<String> originalUuidStrings = uuids.stream()
                .map(UUID::toString)
                .toList();

        List<String> sortedOriginalUuids = originalUuidStrings.stream()
                .sorted()
                .toList();

        List<String> sortedEncodedUuids = encodedUuids.stream()
                .sorted()
                .toList();

        // if original UUIDs are in order, encoded UUIDs should also be in order
        assertThat(originalUuidStrings)
                .as("originalUuidStrings")
                .isEqualTo(sortedOriginalUuids);

        assertThat(encodedUuids)
                .as("encodedUuids")
                .isEqualTo(sortedEncodedUuids);
    }

    /**
     * Data class for UUID test data.
     */
    @Value
    @Builder
    public static class UuidData {

        String uuid;
        String version;

    }

}
