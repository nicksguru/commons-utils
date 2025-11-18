package guru.nicks.commons.cucumber;

import guru.nicks.commons.cloud.domain.CloudFile;
import guru.nicks.commons.cucumber.world.TextWorld;
import guru.nicks.commons.service.CloudFileStorageService;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Step definitions for testing {@link CloudFileStorageService}.
 */
@RequiredArgsConstructor
public class CloudFileStorageServiceSteps {

    // DI
    private final TextWorld textWorld;

    @Spy   // this interface has default methods, they must remain functional
    private CloudFileStorageService cloudFileStorageService;
    private AutoCloseable closeableMocks;

    private String userId;
    private String filename;
    private String fileId;
    private String fileContent;
    private MediaType contentType;
    private Map<String, Object> metadata;
    private CloudFile savedFile;
    private CloudFile foundFile;
    private List<CloudFile> listedFiles;
    private InputStream contentStream;
    private byte[] fileBytes;
    private String checksum;
    private List<CloudFile> expectedFiles;

    @Before
    public void beforeEachScenario() {
        closeableMocks = MockitoAnnotations.openMocks(this);
        metadata = new HashMap<>();
        expectedFiles = new ArrayList<>();
    }

    @After
    public void afterEachScenario() throws Exception {
        closeableMocks.close();
    }

    @DataTableType
    public MetadataEntry createMetadataEntry(Map<String, String> entry) {
        return MetadataEntry.builder()
                .key(entry.get("key"))
                .value(entry.get("value"))
                .build();
    }

    @DataTableType
    public FileEntry createFileEntry(Map<String, String> entry) {
        return FileEntry.builder()
                .filename(entry.get("filename"))
                .contentType(entry.get("contentType"))
                .userId(entry.get("userId"))
                .build();
    }

    @Given("a file with content {string} and filename {string}")
    public void aFileWithContentAndFilename(String content, String name) {
        fileContent = content;
        filename = name;
        fileBytes = content.getBytes(StandardCharsets.UTF_8);
    }

    @Given("a file with content {string}")
    public void aFileWithContent(String content) {
        fileContent = content;
        fileBytes = content.getBytes(StandardCharsets.UTF_8);
    }

    @Given("content type {string}")
    public void contentType(String type) {
        contentType = MediaType.valueOf(type);
    }

    @Given("user ID {string}")
    public void userID(String id) {
        userId = id;
    }

    @Given("metadata")
    public void metadata(List<MetadataEntry> entries) {
        entries.forEach(entry -> metadata.put(entry.getKey(), entry.getValue()));
    }

    @Given("a file with filename {string} exists in storage")
    public void aFileWithFilenameExistsInStorage(String name) {
        filename = name;
        CloudFile file = createCloudFile(name, "file-" + name, userId, contentType);

        when(cloudFileStorageService.findByFilename(name))
                .thenReturn(Optional.of(file));
    }

    @Given("a file with ID {string} exists in storage")
    public void aFileWithIDExistsInStorage(String id) {
        fileId = id;
        CloudFile file = createCloudFile("file-" + id, id, userId, contentType);

        when(cloudFileStorageService.findById(id))
                .thenReturn(Optional.of(file));
    }

    @Given("a file with filename {string} does not exist in storage")
    public void aFileWithFilenameDoesNotExistInStorage(String name) {
        filename = name;

        when(cloudFileStorageService.findByFilename(name))
                .thenReturn(Optional.empty());
    }

    @Given("a file with ID {string} and content {string} exists in storage")
    public void aFileWithIDAndContentExistsInStorage(String id, String content) {
        fileId = id;
        fileContent = content;
        CloudFile file = createCloudFile("file-" + id, id, userId, contentType);

        when(cloudFileStorageService.findById(id))
                .thenReturn(Optional.of(file));

        ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        when(cloudFileStorageService.getInputStream(id))
                .thenReturn(stream);
    }

    @Given("files exist in directory {string}")
    public void filesExistInDirectory(String directory, List<FileEntry> files) {
        expectedFiles = files.stream()
                .map(file -> createCloudFile(
                        file.getFilename(),
                        "id-" + file.getFilename(),
                        file.getUserId(),
                        MediaType.valueOf(file.getContentType())))
                .toList();

        when(cloudFileStorageService.listFiles(directory))
                .thenReturn(expectedFiles);
    }

    @When("the file is saved to storage without metadata")
    public void theFileIsSavedToStorage() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
        CloudFile mockFile = createCloudFile(filename, "generated-id", userId, contentType);

        when(cloudFileStorageService.save(
                eq(userId),
                any(InputStream.class),
                eq(filename),
                eq(contentType),
                anyMap()))
                .thenReturn(mockFile);

        savedFile = cloudFileStorageService.save(userId, inputStream, filename, contentType);
    }

    @When("the file is saved to storage with metadata")
    public void theFileIsSavedToStorageWithMetadata() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
        CloudFile mockFile = createCloudFile(filename, "generated-id", userId, contentType);

        when(cloudFileStorageService.save(
                eq(userId),
                any(InputStream.class),
                eq(filename),
                eq(contentType),
                anyMap()))
                .thenReturn(mockFile);

        savedFile = cloudFileStorageService.save(userId, inputStream, filename, contentType, metadata);
    }

    @When("the file is found by filename")
    public void theFileIsFoundByFilename() {
        Optional<CloudFile> result = cloudFileStorageService.findByFilename(filename);
        foundFile = result.orElse(null);
    }

    @When("the file is found by ID")
    public void theFileIsFoundByID() {
        Optional<CloudFile> result = cloudFileStorageService.findById(fileId);
        foundFile = result.orElse(null);
    }

    @When("the file is requested by filename")
    public void theFileIsRequestedByFilename() {
        var throwable = catchThrowable(() ->
                foundFile = cloudFileStorageService.getByFilename(filename));

        textWorld.setLastException(throwable);
    }

    @When("the file content is requested")
    public void theFileContentIsRequested() {
        contentStream = cloudFileStorageService.getInputStream(fileId);
    }

    @When("files are listed for directory {string}")
    public void filesAreListedForDirectory(String directory) {
        listedFiles = cloudFileStorageService.listFiles(directory);
    }

    @When("the file is deleted by ID")
    public void theFileIsDeletedByID() {
        doNothing().when(cloudFileStorageService).deleteById(fileId);
        cloudFileStorageService.deleteById(fileId);
    }

    @When("the checksum is computed")
    public void theChecksumIsComputed() {
        when(cloudFileStorageService.computeChecksum(fileBytes))
                .thenCallRealMethod();
        checksum = cloudFileStorageService.computeChecksum(fileBytes);
    }

    @Then("the saved file should have the same filename")
    public void theSavedFileShouldHaveTheSameFilename() {
        assertThat(savedFile.getFilename())
                .as("savedFile.filename")
                .isEqualTo(filename);
    }

    @Then("the saved file should have the same content type")
    public void theSavedFileShouldHaveTheSameContentType() {
        assertThat(savedFile.getContentType())
                .as("savedFile.contentType")
                .isEqualTo(contentType);
    }

    @Then("the saved file should have the same user ID")
    public void theSavedFileShouldHaveTheSameUserID() {
        assertThat(savedFile.getUserId())
                .as("savedFile.userId")
                .isEqualTo(userId);
    }

    @Then("the saved file should have a checksum")
    public void theSavedFileShouldHaveAChecksum() {
        assertThat(savedFile.getChecksum())
                .as("savedFile.checksum")
                .isNotNull()
                .isNotEmpty();
    }

    @Then("the saved file should have the metadata")
    public void theSavedFileShouldHaveTheMetadata() {
        verify(cloudFileStorageService).save(
                eq(userId),
                any(InputStream.class),
                eq(filename),
                eq(contentType),
                eq(metadata));
    }

    @Then("the file should be returned")
    public void theFileShouldBeReturned() {
        assertThat(foundFile)
                .as("foundFile")
                .isNotNull();
    }

    @Then("the content should match the original content")
    public void theContentShouldMatchTheOriginalContent() {
        var throwable = catchThrowable(() -> {
            byte[] contentBytes = contentStream.readAllBytes();
            String content = new String(contentBytes, StandardCharsets.UTF_8);
            assertThat(content)
                    .as("content")
                    .isEqualTo(fileContent);
        });

        textWorld.setLastException(throwable);
    }

    @Then("{int} files should be returned")
    public void filesShouldBeReturned(int count) {
        assertThat(listedFiles)
                .as("listedFiles")
                .hasSize(count);
    }

    @Then("the files should have the correct filenames")
    public void theFilesShouldHaveTheCorrectFilenames() {
        List<String> actualFilenames = listedFiles.stream()
                .map(CloudFile::getFilename)
                .toList();

        List<String> expectedFilenames = expectedFiles.stream()
                .map(CloudFile::getFilename)
                .toList();

        assertThat(actualFilenames)
                .as("filenames")
                .containsExactlyInAnyOrderElementsOf(expectedFilenames);
    }

    @Then("the file should be removed from storage")
    public void theFileShouldBeRemovedFromStorage() {
        verify(cloudFileStorageService).deleteById(fileId);
    }

    @Then("the checksum should not be empty")
    public void theChecksumShouldNotBeEmpty() {
        assertThat(checksum)
                .as("checksum")
                .isNotNull()
                .isNotEmpty();
    }

    /**
     * Creates a CloudFile instance with the given parameters.
     *
     * @param filename    the filename
     * @param id          the file ID
     * @param userId      the user ID
     * @param contentType the content type
     * @return a CloudFile instance
     */
    private CloudFile createCloudFile(String filename, String id, String userId, MediaType contentType) {
        return CloudFile.builder()
                .id(id)
                .filename(filename)
                .userId(userId)
                .contentType(contentType)
                .lastModified(Instant.now())
                .size(100L)
                .checksum("test-checksum")
                .build();
    }

    /**
     * Data class for metadata entries in data tables.
     */
    @Value
    @Builder
    public static class MetadataEntry {

        String key;
        String value;

    }

    /**
     * Data class for file entries in data tables.
     */
    @Value
    @Builder
    public static class FileEntry {

        String filename;
        String contentType;
        String userId;

    }
}
