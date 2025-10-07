package guru.nicks.utils;

import guru.nicks.ApplicationContextHolder;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.UtilityClass;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Classpath resource-related utility methods.
 */
@UtilityClass
@Slf4j
public class ResourceUtils {

    private static final ResourcePatternResolver RESOURCE_RESOLVER = new PathMatchingResourcePatternResolver(
            MethodHandles.lookup().lookupClass().getClassLoader());

    private static final Duration CACHE_TTL = Duration.of(1, ChronoUnit.DAYS);

    /**
     * Total size of all entries' {@link CacheEntry#getContent()} the cache can hold before starting eviction.
     */
    private static final DataSize MAX_TOTAL_CACHE_SIZE = DataSize.ofMegabytes(20);

    /**
     * Entries whose {@link CacheEntry#getContent()} size exceeds this value aren't stored in cache.
     */
    private static final DataSize MAX_ENTRY_SIZE = DataSize.ofMegabytes(1);

    /**
     * Keys are resource paths (usually inside JAR, such as {@code /images/file.png}; exactly same paths should be
     * passed to {@link #findAndCacheResource(String, String)}), values are their cached content.
     * <p>
     * Last access (i.e. write or read) extends the cache TTL, so frequently needed keys never expire. It's OK because
     * the values never change.
     */
    private static final Cache<String, CacheEntry> RESOURCE_CACHE = Caffeine.newBuilder()
            .expireAfterAccess(CACHE_TTL)
            // remove expired entries asap (not only when the cache is accessed) to save memory
            .scheduler(Scheduler.systemScheduler())
            // here, max. weight is the total size of all entries' content, in bytes
            .maximumWeight(MAX_TOTAL_CACHE_SIZE.toBytes())
            .weigher(ResourceUtils::computeCacheEntryWeight)
            .build();

    /**
     * @see #getAppBuildTag()
     */
    private static String cachedBuildTag = null;

    /**
     * Loads all files from the given directory (presumably inside JAR) - but not from subdirectories - to in-memory
     * cache. The keys are full filenames, prefixed with {@code directory}.
     *
     * @param directory {@code /some/directory}
     */
    @SneakyThrows
    public static void loadResourcesToCache(String directory) {
        String prefix = normalizePathPrefix(directory);

        Arrays.stream(RESOURCE_RESOLVER.getResources(prefix + "/*"))
                .filter(Resource::exists)
                .forEach(resource -> {
                    String key = prefix + resource.getFilename();
                    CacheEntry cacheEntry = mapToCacheEntry(resource);
                    log.info("Loading resource to in-memory cache: '{}' -> {}", key, cacheEntry);
                    RESOURCE_CACHE.put(key, cacheEntry);
                });
    }

    /**
     * Retrieves resource (presumably from a running JAR). Normalizes filename (without path) to resolve things like
     * {@code ../}, so after prepending the path, it's possible to only go down the path, never up.
     *
     * @param path     path - passed from app config and is trusted
     * @param filename - passed from web requests and is therefore NOT trusted
     * @return optional resource
     */
    public static Optional<Resource> findResource(String path, String filename) {
        String fixedPath = normalizePathPrefix(path);

        // see method comment for explanation of why filename is normalized separately from path
        return Optional.ofNullable(FilenameUtils.normalize(filename, true))
                .filter(StringUtils::isNotBlank)
                .map(file -> fixedPath + file)
                .map(RESOURCE_RESOLVER::getResource)
                .filter(Resource::exists);
    }

    /**
     * Same as {@link #findResource(String, String)}, just throws {@link NoSuchElementException}.
     *
     * @param path     path - passed from app config and is trusted
     * @param filename - passed from web requests and is therefore NOT trusted
     * @return existing resource
     * @throws NoSuchElementException resource not found
     */
    public static Resource getResource(String path, String filename) {
        return findResource(path, filename)
                .orElseThrow(NoSuchElementException::new);
    }

    /**
     * Returns (existing) resources matching the given pattern, for example {@code /images/*} returns all files in
     * {@code /images/} (presumably inside JAR) but not in its subdirectories.
     *
     * @param locationPattern pattern for {@link PathMatchingResourcePatternResolver#getResources(String)}
     * @return resources
     */
    @SneakyThrows
    public static List<Resource> getResources(String locationPattern) {
        return Arrays.stream(RESOURCE_RESOLVER.getResources(locationPattern))
                .filter(Resource::exists)
                .toList();
    }

    /**
     * Calls {@link #getResource(String, String)} and then reads the resource content.
     *
     * @param path     path - passed from app config and is trusted
     * @param filename - passed from web requests and is therefore NOT trusted
     * @return resource content
     * @throws NoSuchElementException resource not found or cannot be read
     */
    public static String getResourceAsString(String path, String filename) {
        Resource resource = getResource(path, filename);
        try {
            return IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new NoSuchElementException("Error reading resource: " + e.getMessage(), e);
        }
    }

    /**
     * Same as {@link #findResource(String, String)} but leverages a cache (gives a 10x speedup). Use this method for
     * frequently needed resources only, otherwise the in-memory cache will be used ineffectively. Why not use Redis?
     * because each time the app starts, it may carry different resources (without changing their name).
     *
     * @param path     path - passed from app config and is trusted
     * @param filename - passed from web requests and is therefore NOT trusted
     * @return existing resource
     * @throws NoSuchElementException resource not found
     */
    @Synchronized
    public static Optional<CacheEntry> findAndCacheResource(String path, String filename) {
        String key = normalizePathPrefix(path) + FilenameUtils.normalize(filename);
        CacheEntry cacheEntry = RESOURCE_CACHE.getIfPresent(key);

        if (cacheEntry == null) {
            cacheEntry = findWithoutCache(key);

            // store in cache only if content size doesn't exceed the maximum (Caffeine doesn't have this feature,
            // therefore LoadingCache can't be used)
            if ((cacheEntry != null)
                    && (cacheEntry.getContent() != null)
                    && (cacheEntry.getContent().length <= MAX_ENTRY_SIZE.toBytes())) {
                RESOURCE_CACHE.put(key, cacheEntry);
            }
        }

        return Optional.ofNullable(cacheEntry);
    }

    /**
     * Same as {@link #getResource(String, String)} but leverages a cache (gives a 10x speedup). Use this method for
     * frequently needed resources only, otherwise the in-memory cache will be used ineffectively. Why not use Redis?
     * because each time the app starts, it may carry different resources (without changing their name).
     *
     * @param path     path - passed from app config and is trusted
     * @param filename - passed from web requests and is therefore NOT trusted
     * @return existing resource
     * @throws NoSuchElementException resource not found
     */
    public static CacheEntry getAndCacheResource(String path, String filename) {
        return findAndCacheResource(path, filename)
                .orElseThrow(NoSuchElementException::new);
    }

    /**
     * Returns a tag built out of {@link GitProperties#getCommitId()}. Or, if there's no such Spring bean or the commit
     * ID is empty, out of {@link BuildProperties#getTime()}. Or, if there's no such Spring bean or the build time is
     * {@code null}, from current time. In all cases, the value is generated once (wrapped in
     * {@link ChecksumUtils#computeJsonChecksumBase64(Object)}), and subsequent calls return the cached copy.
     * <p>
     * If {@link ApplicationContextHolder} is unable to look up Spring beans, the default approach (current time) is
     * used.
     *
     * @return build tag; to use it as part of filenames or URLs, make sure to transform it to e.g. Sha256-hex
     */
    @Synchronized
    public static String getAppBuildTag() {
        if (cachedBuildTag != null) {
            return cachedBuildTag;
        }

        synchronized (ResourceUtils.class) {
            if (cachedBuildTag != null) {
                return cachedBuildTag;
            }

            String newTag = null;

            try {
                newTag = Optional.of(ApplicationContextHolder.getApplicationContext())
                        .map(context -> context.getBean(GitProperties.class))
                        .map(GitProperties::getCommitId)
                        .filter(StringUtils::isNotBlank)
                        .orElse(null);
            } catch (Exception e) {
                // do nothing
            }

            if (newTag == null) {
                try {
                    newTag = Optional.of(ApplicationContextHolder.getApplicationContext())
                            .map(context -> context.getBean(BuildProperties.class))
                            .map(BuildProperties::getTime)
                            .map(Instant::toString)
                            .filter(StringUtils::isNotBlank)
                            .orElse(null);
                } catch (Exception e) {
                    // do nothing
                }
            }

            // no build info - just use current time
            if (newTag == null) {
                newTag = Instant.now().toString();
            }

            cachedBuildTag = ChecksumUtils.computeJsonChecksumBase64(newTag);
            return cachedBuildTag;
        }
    }

    /**
     * Guesses content type by file extension, falls back to {@link MediaType#APPLICATION_OCTET_STREAM} on failure.
     *
     * @param filename filename
     * @return content type
     */
    public static MediaType guessContentType(@Nullable String filename) {
        String contentType = FilenameUtils.getExtension(filename);

        // file extension may be null
        if ("jpg".equalsIgnoreCase(contentType) || "jpeg".equalsIgnoreCase(contentType)) {
            return MediaType.IMAGE_JPEG;
        } else if ("png".equalsIgnoreCase(contentType)) {
            return MediaType.IMAGE_PNG;
        } else if ("gif".equalsIgnoreCase(contentType)) {
            return MediaType.IMAGE_GIF;
        } else {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * Does the same as {@link FilenameUtils#normalizeNoEndSeparator(String)}, then appends {@link File#separator} if
     * needed, so filenames can be appended to it directly.
     *
     * @param path path (WITHOUT a filename - use {@link FilenameUtils#getFullPath(String)} to extract it if needed)
     * @return normalized path - always ends with {@link File#separator}
     */
    public static String normalizePathPrefix(String path) {
        String fixedPath = Optional.ofNullable(FilenameUtils.normalizeNoEndSeparator(path)).orElse("");

        // the above call removes trailing '/', but '/' remains as-is
        if (!fixedPath.endsWith(File.separator)) {
            fixedPath = fixedPath + File.separator;
        }

        return fixedPath;
    }

    /**
     * Called if a cache entry is missing.
     *
     * @param key cache key
     * @return cache entry or {@code null}, as required by {@link CacheLoader#load(Object)}
     */
    @SneakyThrows
    @Nullable
    private static CacheEntry findWithoutCache(String key) {
        var file = new File(key);
        String path = file.getParent();
        String filename = file.getName();

        return findResource(path, filename)
                .map(ResourceUtils::mapToCacheEntry)
                .orElse(null);
    }

    @SneakyThrows
    private static CacheEntry mapToCacheEntry(Resource resource) {
        byte[] content = IOUtils.toByteArray(resource.getInputStream());
        String checksum = ChecksumUtils.computeJsonChecksumBase64(content);
        MediaType contentType = guessContentType(resource.getFilename());

        return CacheEntry.builder()
                .content(content)
                .contentType(contentType)
                .contentLength(content.length)
                .checksum(checksum)
                .build();
    }

    /**
     * Computes weight which is content length in bytes.
     *
     * @param key   cache key
     * @param value cache entry
     * @return size of {@link CacheEntry#getContent()}
     */
    private static int computeCacheEntryWeight(String key, CacheEntry value) {
        return Optional.ofNullable(value.getContent())
                .map(array -> array.length)
                .orElse(0);
    }

    @Value
    @NonFinal
    @Jacksonized
    @Builder(toBuilder = true)
    public static class CacheEntry {

        @ToString.Exclude
        byte[] content;

        long contentLength;
        MediaType contentType;
        String checksum;

    }

}
