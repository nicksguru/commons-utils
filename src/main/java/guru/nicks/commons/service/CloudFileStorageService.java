package guru.nicks.commons.service;

import guru.nicks.commons.cloud.domain.CloudFile;
import guru.nicks.commons.exception.http.NotFoundException;

import jakarta.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.MediaType;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Files stored in the cloud or a local DB.
 */
public interface CloudFileStorageService {

    String METADATA_USER_ID = "userId";
    String METADATA_CHECKSUM = "checksum";

    /**
     * Inserts file into storage or replaces one with the same name. The {@code userId} is saved in
     * {@link CloudFile#getUserId()}. Checksum is computed and saved in {@link CloudFile#getChecksum()}.
     * <p>
     * WARNING: stream must be read fully in order to calculate its checksum, however no more than
     * {@link Integer#MAX_VALUE} bytes can be read.
     *
     * @param userId      file owner ID, will be stored in file metadata, can be {@code null}
     * @param inputStream file content
     * @param filename    object ID, format depends on storage, such as S3 URI, MongoGridFS virtual filename
     * @param contentType content type; call {@code new MediaType("ab", "cd")} to create new type {@code ab/cd} or
     *                    {@link MediaType#valueOf(String)} to parse a string
     * @param metadata    arbitrary metadata to attach to file (not all storage backends may support metadata)
     * @return file
     * @throws IllegalArgumentException stream size is larger than {@link Integer#MAX_VALUE}
     */
    CloudFile save(@Nullable String userId, InputStream inputStream, String filename, MediaType contentType,
            Map<String, ?> metadata);

    /**
     * Does the same as {@link #save(String, InputStream, String, MediaType, Map)}, just sets empty metadata.
     *
     * @param userId      file owner ID, will be stored in file metadata, can be {@code null}
     * @param inputStream file content
     * @param filename    object ID, format depends on storage: S3 URL, MongoGridFS virtual filename etc.
     * @param contentType content type; call {@code new MediaType("ab", "cd")} to create new type {@code ab/cd} or
     *                    {@link MediaType#valueOf(String)} to parse a string
     * @return file
     */
    default CloudFile save(@Nullable String userId, InputStream inputStream, String filename, MediaType contentType) {
        return save(userId, inputStream, filename, contentType, Collections.emptyMap());
    }

    /**
     * Finds the most recently uploaded file version by its filename. Does not fetch content.
     *
     * @param filename filename, as was passed to {@link #save(String, InputStream, String, MediaType, Map)}
     * @return file
     * @see #getInputStream(String)
     */
    Optional<CloudFile> findByFilename(String filename);

    /**
     * Calls {@link #findByFilename(String)} and throws exception if not found. Does not fetch content.
     *
     * @param filename filename, as was passed to {@link #save(String, InputStream, String, MediaType, Map)}
     * @return file
     * @throws NotFoundException file not found
     * @see #getInputStream(String)
     */
    default CloudFile getByFilename(String filename) {
        return findByFilename(filename).orElseThrow(() -> new NotFoundException("File not found"));
    }

    /**
     * Finds file by its ID. Files are, generally, multi-versioned, so this ID points to a concrete version. Does not
     * fetch content.
     *
     * @param id object ID, format depends on storage: S3 URL, MongoGridFS ID etc.
     * @return file
     * @see #getInputStream(String)
     */
    Optional<CloudFile> findById(String id);

    /**
     * Calls {@link #findById(String)} and throws exception if not found. Does not fetch content.
     *
     * @param id object ID, format depends on storage: S3 URL, MongoGridFS ID etc.
     * @return file
     * @throws NotFoundException file not found
     * @see #getInputStream(String)
     */
    default CloudFile getById(String id) {
        return findById(id).orElseThrow(NotFoundException::new);
    }

    /**
     * Returns input stream for existing file.
     *
     * @param id object ID, format depends on storage: S3 URL, MongoGridFS ID etc.
     * @return input stream
     * @throws NotFoundException filed not found or inaccessible
     */
    InputStream getInputStream(String id);

    /**
     * Lists virtual directory contents. Subdirectories are not traversed.
     *
     * @param path '/*' will be appended
     * @return files found
     */
    List<CloudFile> listFiles(String path);

    /**
     * Deletes file from storage by its {@link CloudFile#getId()}. Files are, generally, multi-versioned, so this ID
     * points to a concrete version. If there's no such file, method does nothing.
     *
     * @param id object ID
     */
    void deleteById(String id);

    /**
     * Computes content checksum to be stored in storage metadata.
     *
     * @param content file content
     * @return default implementation returns SHA-256 hex-encoded
     */
    default String computeChecksum(byte[] content) {
        return DigestUtils.sha256Hex(content);
    }

}
