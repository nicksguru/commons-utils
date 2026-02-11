package guru.nicks.commons.serializer;

import jakarta.annotation.Nullable;
import one.nio.serial.DeserializeStream;
import one.nio.serial.PersistStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationException;
import org.springframework.context.annotation.Primary;

import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Serializes / deserializes / clones objects using OneNio. To switch to another implementation, replace this bean with
 * a {@link Primary @Primary} one.
 * <p>
 * WARNING: as per <a href="https://github.com/odnoklassniki/one-nio/wiki/Serialization-FAQ">this article</a>, only
 * classes implementing {@link Serializable} / {@link Externalizable} interfaces are serialized, others fail with
 * 'Invalid serializer' error. {@link Collection}, {@link Map}, {@link Enum}, and primitives are serialized seamlessly.
 * <p>
 * For example, {@link UUID} is not serializable (and therefore not {@code @Cacheable}) because it's an object which is
 * not {@link Serializable}.
 */
public class OneNioSerializer implements NativeJavaSerializer {

    @Override
    public <T> byte[] serialize(@Nullable T obj) throws SerializationException {
        if (obj == null) {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }

        try (var stream = new PersistStream(1024)) {
            stream.writeObject(obj);
            return stream.toByteArray();
        } catch (IOException e) {
            throw new SerializationException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T deserialize(@Nullable byte[] bytes) throws SerializationException {
        if ((bytes == null) || (bytes.length == 0)) {
            return null;
        }

        try (var stream = new DeserializeStream(bytes)) {
            return (T) stream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException(e.getMessage(), e);
        }
    }

}
