package guru.nicks.commons.utils.compressor;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

@UtilityClass
public class ZstdCompressorUtils {

    private static final int COMPRESSION_LEVEL = 3;

    private static final boolean GENERATE_CHECKSUM = true;

    /**
     * Hard upper bound for a single decompressed payload, enforced explicitly instead of implicitly.
     */
    private static final int MAX_DECOMPRESSED_SIZE = 50 * (int) FileUtils.ONE_MB;

    public byte[] compress(byte[] source) {
        try (ZstdCompressCtx ctx = new ZstdCompressCtx()) {
            ctx.setLevel(COMPRESSION_LEVEL);
            ctx.setChecksum(GENERATE_CHECKSUM);
            return ctx.compress(source);
        }
    }

    /**
     * Decompresses a Zstd frame, allocating exactly the frame's declared content size instead of a fixed 10 MiB buffer.
     * Frames produced by {@link #compress(byte[])} embed the content size, so they take the exact-allocation fast
     * path.
     *
     * @param compressed compressed payload
     * @return decompressed payload
     * @throws IllegalArgumentException frame content size is unknown (e.g. produced by streaming compression without a
     *                                  pledged size), invalid (not a Zstd frame) or exceeds
     *                                  {@link #MAX_DECOMPRESSED_SIZE}
     */
    public byte[] decompress(byte[] compressed) {
        long frameContentSize = Zstd.getFrameContentSize(compressed);

        // negative means 'unknown' (-1) or 'not a Zstd frame' (-2), as per zstd-jni
        if ((frameContentSize < 0) || (frameContentSize > MAX_DECOMPRESSED_SIZE)) {
            throw new IllegalArgumentException("Zstd frame content size is unknown, or invalid, or exceeds "
                    + MAX_DECOMPRESSED_SIZE + " bytes: " + frameContentSize);
        }

        return Zstd.decompress(compressed, (int) frameContentSize);
    }

}
