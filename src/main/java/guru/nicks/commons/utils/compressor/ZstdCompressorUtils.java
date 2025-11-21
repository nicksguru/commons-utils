package guru.nicks.commons.utils.compressor;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

@UtilityClass
public class ZstdCompressorUtils {

    private static final int COMPRESSION_LEVEL = 3;

    private static final boolean GENERATE_CHECKSUM = true;

    public byte[] compress(byte[] source) {
        try (ZstdCompressCtx ctx = new ZstdCompressCtx()) {
            ctx.setLevel(COMPRESSION_LEVEL);
            ctx.setChecksum(GENERATE_CHECKSUM);
            return ctx.compress(source);
        }
    }

    /**
     * WARNING: the maximum decompressed size is limited to 10Mb because the output buffer is pre-allocated.
     */
    public byte[] decompress(byte[] compressed) {
        return Zstd.decompress(compressed, 10 * (int) FileUtils.ONE_MB);
    }

}
