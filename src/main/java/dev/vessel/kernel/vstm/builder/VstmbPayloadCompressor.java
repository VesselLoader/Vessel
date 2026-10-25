package dev.vessel.kernel.vstm.builder;

import dev.vessel.kernel.vstm.binary.VstmbBinaryCorruptException;
import java.io.ByteArrayOutputStream;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Payload compression engine for string pools and metadata blocks in .vstmb distribution files.
 *
 * <p>Employs high-ratio, zero-dependency streaming compression with pure Java deflate/inflate algorithms,
 * ensuring complete cross-platform predictability on Linux, Windows, and macOS without native library
 * linker dependencies (Catalog refs: ZER-0120, STO-0012).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmbPayloadCompressor {

    private static final int BUFFER_SIZE = 8192;

    /**
     * Compresses raw binary payload bytes using maximum compression level.
     *
     * @param rawData uncompressed input bytes
     * @return compressed byte array
     */
    public static byte[] compress(byte[] rawData) {
        Objects.requireNonNull(rawData, "rawData must not be null");
        if (rawData.length == 0) {
            return new byte[0];
        }

        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(rawData);
            deflater.finish();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(rawData.length / 2 + 64);
            byte[] buffer = new byte[BUFFER_SIZE];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            return outputStream.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /**
     * Decompresses deflated payload bytes back to original uncompressed form.
     *
     * @param compressedData compressed input bytes
     * @param expectedSize   the known uncompressed size, or 0 if unknown
     * @return decompressed raw byte array
     * @throws VstmbBinaryCorruptException if data stream is corrupt or truncated
     */
    public static byte[] decompress(byte[] compressedData, int expectedSize) {
        Objects.requireNonNull(compressedData, "compressedData must not be null");
        if (compressedData.length == 0) {
            return new byte[0];
        }

        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressedData);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(expectedSize > 0 ? expectedSize : compressedData.length * 2);
            byte[] buffer = new byte[BUFFER_SIZE];

            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && inflater.needsInput()) {
                    break;
                }
                outputStream.write(buffer, 0, count);
            }

            return outputStream.toByteArray();
        } catch (DataFormatException e) {
            throw new VstmbBinaryCorruptException("ERR-VSTM-DECOMPRESS", "Decompression failure in .vstmb payload", e);
        } finally {
            inflater.end();
        }
    }

    private VstmbPayloadCompressor() {
        // Compression utility
    }
}
