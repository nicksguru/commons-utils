package guru.nicks.commons.cache;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.cache.interceptor.SimpleKeyGenerator;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Builds a cache key out of all calling method's arguments, stringified as {@link Objects#toString(Object, String)}
 * would (with {@code null} encoded as the literal {@code null} string), using an unambiguous length-prefixed encoding:
 * argument count, then each argument's length followed by its value, for example {@code 2|6:param16:param2}. To be used
 * as {@link Cacheable#keyGenerator()}.
 * <p>
 * Length prefixing makes the encoding injective: no two distinct argument tuples (such as {@code ("a::b", "c")} and
 * {@code ("a", "b::c")}, or {@code (null, null)} and {@code ("", "")}) can encode to the same key, so different
 * invocations never silently share a cached result.
 * <p>
 * Default key generator ({@link SimpleKeyGenerator}) leverages {@link SimpleKey} which stringifies all method arguments
 * too, but the key it creates is a monolith {@link SimpleKey#toString()} value having no folder-like structure:
 * {@code SimpleKey ["arg1", "arg2", ...]}.
 * <p>
 * NOTE: the key format changed from plain {@code ::} joining to this length-prefixed encoding - caches persisted with
 * the old format are invalidated once, which is an acceptable one-time cost.
 */
public class ToStringJoiningCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        // length-prefixed parts make the delimiter unambiguous: no two distinct argument
        // tuples can encode to the same key
        var builder = new StringBuilder().append(params.length).append('|');

        for (Object param : params) {
            String value = (param == null) ? "null" : param.toString();
            builder.append(value.length()).append(':').append(value);
        }

        return builder.toString();
    }

}
