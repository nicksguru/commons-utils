package guru.nicks.serializer;

import jakarta.annotation.Nullable;
import one.nio.serial.DeserializeStream;
import one.nio.serial.PersistStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Serializes / deserializes / clones objects using OneNio.
 */
@Component
@Primary
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
