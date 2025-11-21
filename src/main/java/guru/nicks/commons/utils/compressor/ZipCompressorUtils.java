package guru.nicks.commons.utils.compressor;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@UtilityClass
public class ZipCompressorUtils {

    @SneakyThrows(IOException.class)
    public static byte[] compress(byte[] source) {
        try (var finalStream = new ByteArrayOutputStream();
                var compressingStream = new ZipOutputStream(finalStream)) {
            // create a dummy entry (Zip can't compress unnamed bytes)
            var zipEntry = new ZipEntry("data");
            compressingStream.putNextEntry(zipEntry);

            compressingStream.write(source);
            // need to close this stream explicitly, otherwise it's not flushed before the next line of code is executed
            compressingStream.close();
            return finalStream.toByteArray();
        }
    }

    @SneakyThrows(IOException.class)
    public static byte[] decompress(byte[] compressed) {
        try (var finalStream = new ByteArrayOutputStream();
                var decompressingStream = new ZipInputStream(new ByteArrayInputStream(compressed))) {
            ZipEntry zipEntry = decompressingStream.getNextEntry();

            if (zipEntry == null) {
                throw new IOException("No entries found in ZIP data");
            }

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
