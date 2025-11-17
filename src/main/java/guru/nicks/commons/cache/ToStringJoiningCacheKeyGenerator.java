package guru.nicks.commons.cache;

import guru.nicks.commons.cache.domain.CacheConstants;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Joins all calling method's arguments with {@link CacheConstants#TOPIC_DELIMITER} as
 * {@link Objects#toString(Object, String)}, replacing nulls with empty strings ({@code :: ::} and {@code ::::} keys are
 * valid and different - it's important to not lose any arguments). To be used as {@link Cacheable#keyGenerator()}.
 * <p>
 * Default key generator ({@link SimpleKeyGenerator}) leverages {@link SimpleKey} which stringifies all method arguments
 * too, but the key it creates is a monolith {@link SimpleKey#toString()} value having no folder-like structure:
 * {@code SimpleKey ["arg1", "arg2", ...]}.
 */
@Component
public class ToStringJoiningCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        return Arrays.stream(params)
                .map(obj -> Objects.toString(obj, ""))
                .collect(Collectors.joining(CacheConstants.TOPIC_DELIMITER));
    }

}
