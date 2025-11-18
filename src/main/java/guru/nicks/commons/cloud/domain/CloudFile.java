package guru.nicks.commons.cloud.domain;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.jackson.Jacksonized;
import org.springframework.http.MediaType;

import java.time.Instant;

/**
 * A file in the cloud.
 */
@Value
@NonFinal
@Jacksonized
@Builder(toBuilder = true)
public class CloudFile {

    /**
     * Files may be, depending on the provider, multi-versioned, in which case this ID points to a concrete version. For
     * example, for AWS S3 this is 's3://bucket/path/to/file', for MongoGridFS - object ID in DB.
     */
    String id;

    /**
     * If storage supports multi-versioning, this field is not unique - {@link #getId()} is. Otherwise, every filename
     * is identical to {@link #getId()}.
     */
    String filename;

    /**
     * File owner.
     */
    String userId;

    MediaType contentType;
    Instant lastModified;
    Long size;

    /**
     * Content checksum.
     */
    String checksum;

}
