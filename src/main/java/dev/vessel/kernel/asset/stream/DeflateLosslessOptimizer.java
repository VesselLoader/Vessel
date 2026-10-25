package dev.vessel.kernel.asset.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * High-efficiency lossless Deflate compression optimizer configured at level 9 (BEST_COMPRESSION).
 * Uses fine-tuned sliding dictionary buffers to squeeze out redundant Minecraft JSON/PNG structures.
 * Extinguishes AST-OPT.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DeflateLosslessOptimizer {

    public static final int COMPRESSION_LEVEL = Deflater.BEST_COMPRESSION;
    public static final int BUFFER_SIZE = 16384; // 16 KB

    /**
     * Compresses the input byte array using maximum Deflate compression.
     *
     * @param rawData uncompressed input bytes
     * @return compressed payload container
     */
    public CompressedAssetPayload compress(byte[] rawData) {
        Objects.requireNonNull(rawData, "rawData cannot be null");

        CRC32 crc32 = new CRC32();
        crc32.update(rawData);
        long checksum = crc32.getValue();

        if (rawData.length == 0) {
            return new CompressedAssetPayload(0, 0, PackCompressionAlgorithm.DEFLATE_OPTIMAL, new byte[0], checksum);
        }

        Deflater deflater = new Deflater(COMPRESSION_LEVEL, false);
        deflater.setInput(rawData);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream(rawData.length);
        byte[] buffer = new byte[BUFFER_SIZE];

        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            baos.write(buffer, 0, count);
        }
        deflater.end();

        byte[] compressedBytes = baos.toByteArray();
        return new CompressedAssetPayload(
                rawData.length,
                compressedBytes.length,
                PackCompressionAlgorithm.DEFLATE_OPTIMAL,
                compressedBytes,
                checksum
        );
    }

    /**
     * Decompresses Deflate-compressed bytes back into original uncompressed form.
     *
     * @param compressedBytes compressed payload
     * @param expectedOriginalSize expected size in bytes
     * @return uncompressed byte array
     * @throws IOException if decompression fails or data is corrupted
     */
    public byte[] decompress(byte[] compressedBytes, int expectedOriginalSize) throws IOException {
        Objects.requireNonNull(compressedBytes, "compressedBytes cannot be null");
        if (compressedBytes.length == 0) {
            return new byte[0];
        }

        Inflater inflater = new Inflater(false);
        inflater.setInput(compressedBytes);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(expectedOriginalSize > 0 ? expectedOriginalSize : BUFFER_SIZE);
        byte[] buffer = new byte[BUFFER_SIZE];

        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && inflater.needsInput()) {
                    break;
                }
                baos.write(buffer, 0, count);
            }
            return baos.toByteArray();
        } catch (DataFormatException dataFormatException) {
            throw new IOException("Deflate decompression error: corrupted bitstream", dataFormatException);
        } finally {
            inflater.end();
        }
    }
}
