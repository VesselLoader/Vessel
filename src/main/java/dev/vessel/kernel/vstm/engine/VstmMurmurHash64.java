package dev.vessel.kernel.vstm.engine;

import dev.vessel.kernel.vstm.binary.VstmbOffHeapByteReader;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 64-bit MurmurHash algorithm (MurmurHash64A) optimized for JVM identifiers and off-heap slices.
 *
 * <p>Produces extremely uniform hash distributions across class names, field names,
 * and method descriptors with near-zero collision probability in $100,000+$ item spaces
 * (Catalog refs: ADV-0320, ZER-0010).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmMurmurHash64 {

    private static final long M = 0xc6a4a7935bd1e995L;
    private static final int R = 47;
    public static final long DEFAULT_SEED = 0xe17a1465L;

    /**
     * Hashes a Java String with the default VSTM seed.
     *
     * @param input the string to hash
     * @return 64-bit hash
     */
    public static long hash64(String input) {
        return hash64(input, DEFAULT_SEED);
    }

    /**
     * Hashes a Java String with a caller-specified seed.
     *
     * @param input the string to hash
     * @param seed  the seed
     * @return 64-bit hash
     */
    public static long hash64(String input, long seed) {
        Objects.requireNonNull(input, "input string must not be null");
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        return hash64(bytes, 0, bytes.length, seed);
    }

    /**
     * Hashes a byte array range using MurmurHash64A.
     *
     * @param data   the byte array
     * @param offset the starting offset
     * @param length the number of bytes
     * @param seed   the 64-bit seed
     * @return 64-bit hash
     */
    @SuppressWarnings("fallthrough")
    public static long hash64(byte[] data, int offset, int length, long seed) {
        Objects.requireNonNull(data, "data buffer must not be null");
        if (offset < 0 || length < 0 || offset + length > data.length) {
            throw new IndexOutOfBoundsException("Invalid range: offset=" + offset + ", length=" + length);
        }

        long h = seed ^ (length * M);
        int nblocks = length >> 3; // length / 8

        for (int i = 0; i < nblocks; i++) {
            int index = offset + (i << 3);
            long k = ((long) data[index] & 0xffL)
                    | (((long) data[index + 1] & 0xffL) << 8)
                    | (((long) data[index + 2] & 0xffL) << 16)
                    | (((long) data[index + 3] & 0xffL) << 24)
                    | (((long) data[index + 4] & 0xffL) << 32)
                    | (((long) data[index + 5] & 0xffL) << 40)
                    | (((long) data[index + 6] & 0xffL) << 48)
                    | (((long) data[index + 7] & 0xffL) << 56);

            k *= M;
            k ^= k >>> R;
            k *= M;

            h ^= k;
            h *= M;
        }

        int tailIndex = offset + (nblocks << 3);
        int remaining = length & 7;

        switch (remaining) {
            case 7:
                h ^= ((long) data[tailIndex + 6] & 0xffL) << 48;
            case 6:
                h ^= ((long) data[tailIndex + 5] & 0xffL) << 40;
            case 5:
                h ^= ((long) data[tailIndex + 4] & 0xffL) << 32;
            case 4:
                h ^= ((long) data[tailIndex + 3] & 0xffL) << 24;
            case 3:
                h ^= ((long) data[tailIndex + 2] & 0xffL) << 16;
            case 2:
                h ^= ((long) data[tailIndex + 1] & 0xffL) << 8;
            case 1:
                h ^= ((long) data[tailIndex] & 0xffL);
                h *= M;
            default:
                break;
        }

        h ^= h >>> R;
        h *= M;
        h ^= h >>> R;

        return h;
    }

    /**
     * Hashes an off-heap {@link MemorySegment} slice without heap allocation.
     *
     * @param segment the memory segment
     * @param offset  starting byte offset
     * @param length  length in bytes
     * @param seed    the 64-bit seed
     * @return 64-bit hash
     */
    public static long hash64(MemorySegment segment, long offset, long length, long seed) {
        VstmbOffHeapByteReader.checkBounds(segment, offset, length);

        long h = seed ^ (length * M);
        long nblocks = length >> 3;

        for (long i = 0; i < nblocks; i++) {
            long currentOffset = offset + (i << 3);
            long k = VstmbOffHeapByteReader.readLong(segment, currentOffset);

            k *= M;
            k ^= k >>> R;
            k *= M;

            h ^= k;
            h *= M;
        }

        long tailOffset = offset + (nblocks << 3);
        int remaining = (int) (length & 7);

        for (int i = remaining - 1; i >= 0; i--) {
            byte b = VstmbOffHeapByteReader.readByte(segment, tailOffset + i);
            h ^= ((long) b & 0xffL) << (i << 3);
            if (i == 0) {
                h *= M;
            }
        }

        h ^= h >>> R;
        h *= M;
        h ^= h >>> R;

        return h;
    }

    private VstmMurmurHash64() {
        // Hashing algorithm utility
    }
}
