package dev.vessel.kernel.network.bridge;

import java.util.Arrays;
import java.util.Objects;

/**
 * Run-Length Encoding (RLE) compressor for Chisels &amp; Bits and LittleTiles 4096-bit (64-long)
 * voxel occupancy bitmasks, cutting microblock chunk packet bandwidth by up to 90%.
 *
 * @version 1.0.0
 */
public final class MicroblockDeltaCompressor {

    /**
     * Compresses an array of 64-bit voxel bitmask words using run-length encoding into a flat
     * {@code int[]} array formatted as {@code [runLength, high32Bits, low32Bits, ...]}.
     *
     * @param raw64Words the raw 64-bit voxel bitmask array
     * @return the RLE-compressed {@code int[]} stream
     */
    public int[] compressVoxelBitmaskRle(long[] raw64Words) {
        Objects.requireNonNull(raw64Words, "raw64Words must not be null");
        if (raw64Words.length == 0) {
            return new int[0];
        }
        int[] buffer = new int[raw64Words.length * 3];
        int writeIdx = 0;
        long currentWord = raw64Words[0];
        int runLength = 1;

        for (int i = 1; i < raw64Words.length; i++) {
            if (raw64Words[i] == currentWord) {
                runLength++;
            } else {
                buffer[writeIdx++] = runLength;
                buffer[writeIdx++] = (int) (currentWord >>> 32);
                buffer[writeIdx++] = (int) currentWord;
                currentWord = raw64Words[i];
                runLength = 1;
            }
        }
        buffer[writeIdx++] = runLength;
        buffer[writeIdx++] = (int) (currentWord >>> 32);
        buffer[writeIdx++] = (int) currentWord;
        return Arrays.copyOf(buffer, writeIdx);
    }

    /**
     * Decompresses an RLE-encoded {@code int[]} stream back into a {@code long[]} voxel bitmask
     * array of length {@code expectedWordCount}.
     *
     * @param rleCompressed     the RLE-compressed {@code int[]} stream
     * @param expectedWordCount the expected number of 64-bit words in the output array
     * @return the reconstructed {@code long[]} voxel bitmask array
     */
    public long[] decompressVoxelBitmaskRle(int[] rleCompressed, int expectedWordCount) {
        Objects.requireNonNull(rleCompressed, "rleCompressed must not be null");
        int safeCount = Math.max(0, expectedWordCount);
        long[] output = new long[safeCount];
        int outIdx = 0;

        for (int i = 0; (i + 2) < rleCompressed.length && outIdx < safeCount; i += 3) {
            int runLength = Math.max(0, rleCompressed[i]);
            long high = ((long) rleCompressed[i + 1]) << 32;
            long low = ((long) rleCompressed[i + 2]) & 0xFFFFFFFFL;
            long word = high | low;
            for (int r = 0; r < runLength && outIdx < safeCount; r++) {
                output[outIdx++] = word;
            }
        }
        return output;
    }
}
