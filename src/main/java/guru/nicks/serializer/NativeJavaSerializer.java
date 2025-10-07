package guru.nicks.serializer;

import jakarta.annotation.Nullable;
import org.apache.commons.lang3.SerializationException;

import java.io.Externalizable;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;

/**
 * Serializes / deserializes / clones objects. Use as a bean to call the preferred implementation without knowing the
 * details.
 * <p>
 * JSON can't handle any class seamlessly - it fails on certain subclasses or immutable collections. The final choice
 * was made based on
 * <a href="https://habr-com.translate.goog/en/companies/sberbank/articles/488612/?_x_tr_sl=auto&_x_tr_tl=en-US">this
 * research</a>. Namely, OneNio produces bigger data (1.5Kb vs. 1Kb for FST for the same small object), but it's far
 * more flexible in terms of loading old cached data to a new class version, casting cached {@code int} to actual
 * {@code long} etc. Also, FST project seems somewhat abandoned, for example it doesn't support {@link Instant}.
 * <p>
 * WARNING: as per <a href="https://github.com/odnoklassniki/one-nio/wiki/Serialization-FAQ">this article</a>, only
 * classes implementing {@link Serializable} / {@link Externalizable} interfaces are serialized, others fail with
 * 'Invalid serializer' error. {@link Collection}, {@link Map}, {@link Enum}, and primitives are serialized seamlessly.
 */
public interface NativeJavaSerializer {

    /**
     * Clones object by first serializing it and then deserializing.
     *
     * @param obj object to clone
     * @param <T> object type
     * @return cloned object
     * @throws SerializationException something went wrong
     */
    @Nullable
    default <T> T clone(@Nullable T obj) throws SerializationException {
        return deserialize(serialize(obj));
    }

    /**
     * Serializes object.
     *
     * @param obj object to serialize
     * @param <T> object type
     * @return serialized object
     * @throws SerializationException something went wrong
     */
    <T> byte[] serialize(@Nullable T obj) throws SerializationException;

    /**
     * Deserializes object.
     *
     * @param bytes data to serialize
     * @param <T>   object type
     * @return deserialized object
     * @throws SerializationException something went wrong
     */
    @Nullable
    <T> T deserialize(@Nullable byte[] bytes) throws SerializationException;

}
