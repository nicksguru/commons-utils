package guru.nicks.commons.utils.compressor;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@UtilityClass
public class GzipCompressorUtils {

    @SneakyThrows(IOException.class)
    public static byte[] compress(byte[] source) {
        try (var finalStream = new ByteArrayOutputStream();
                var compressingStream = new GZIPOutputStream(finalStream)) {
            compressingStream.write(source);
            // need to close this stream explicitly, otherwise it's not flushed before the next line of code is executed
            compressingStream.close();
            return finalStream.toByteArray();
        }
    }

    @SneakyThrows(IOException.class)
    public static byte[] decompress(byte[] compressed) {
        try (var finalStream = new ByteArrayOutputStream();
                var inputStream = new ByteArrayInputStream(compressed);
                var decompressingStream = new GZIPInputStream(inputStream)) {
            byte[] buffer = new byte[1024];
            int length;

            // read decompressed data fully
            while ((length = decompressingStream.read(buffer)) > 0) {
                finalStream.write(buffer, 0, length);
            }

            return finalStream.toByteArray();
        }
    }

}
