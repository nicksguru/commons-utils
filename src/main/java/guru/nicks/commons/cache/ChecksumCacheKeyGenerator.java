package guru.nicks.commons.cache;

import guru.nicks.commons.cache.domain.CacheConstants;
import guru.nicks.commons.utils.crypto.ChecksumUtils;
import guru.nicks.commons.utils.crypto.HashUtils;
import guru.nicks.commons.utils.json.JsonUtils;

import jakarta.annotation.Nullable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Joins all calling method's arguments with {@link CacheConstants#TOPIC_DELIMITER}, each argument hashed with
 * {@link HashUtils#XXHASH3} over its canonical JSON ({@link JsonUtils#sortObjectKeys(Object)} - sorted keys and set
 * elements, which makes the key deterministic for equal arguments), replacing nulls with hashes of empty strings;
 * {@code :: ::} and {@code ::::} keys are valid and different - it's important to not lose any arguments. To be used as
 * {@link Cacheable#keyGenerator()}.
 * <p>
 * XXHASH3 is used instead of a cryptographic hash (as in {@link ChecksumUtils#computeJsonChecksum(Object)}) because a
 * cache key needs speed and determinism, not cryptographic strength: the key is computed on every call, cache hits
 * included, and must cost less than the cached method itself.
 */
public class ChecksumCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        return Arrays.stream(params)
                .map(this::computeKeyPart)
                .collect(Collectors.joining(CacheConstants.TOPIC_DELIMITER));
    }

    /**
     * Computes a single argument's key part: XXHASH3 hex digest of the argument's canonical JSON.
     *
     * @param param method argument (can be null)
     * @return lowercase hex hash of canonical JSON (of an empty string for a null argument)
     */
    private String computeKeyPart(@Nullable Object param) {
        // canonical JSON (sorted keys and set elements) is what makes the key deterministic for equal arguments
        String canonicalJson = (param == null)
                ? ""
                : JsonUtils.sortObjectKeys(param);

        return HashUtils.XXHASH3.computeHex(
                canonicalJson.getBytes(StandardCharsets.UTF_8));
    }

}
