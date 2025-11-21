package guru.nicks.commons.cache;

import guru.nicks.commons.cache.domain.CacheConstants;
import guru.nicks.commons.utils.crypto.ChecksumUtils;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Joins all calling method's arguments with {@link CacheConstants#TOPIC_DELIMITER} as
 * {@link ChecksumUtils#computeJsonChecksumBase64(Object)}, replacing nulls with empty strings ({@code :: ::} and
 * {@code ::::} keys are valid and different - it's important to not lose any arguments). To be used as
 * {@link Cacheable#keyGenerator()}.
 */
@Component
public class ChecksumCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        return Arrays.stream(params)
                .map(ChecksumUtils::computeJsonChecksumBase64)
                .collect(Collectors.joining(CacheConstants.TOPIC_DELIMITER));
    }

}
