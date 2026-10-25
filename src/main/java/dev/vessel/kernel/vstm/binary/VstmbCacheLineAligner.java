package dev.vessel.kernel.vstm.binary;

/**
 * Ensures table pointers, hash slots, and string pools in .vstmb binary files are aligned
 * to standard 64-byte L1/L2 cache line boundaries.
 *
 * <p>Hardware Rationale:
 * Misaligned memory access across cache line boundaries introduces CPU pipeline stalls,
 * dual memory fetches, and false sharing in multi-threaded regional workers.
 * Aligning to 64 bytes maximizes L1 cache line hit rates and unlocks SIMD prefetching
 * (Catalog refs: VOL6-0045, ZER-0050, DVI-00142).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmbCacheLineAligner {

    /**
     * Standard modern CPU L1/L2 cache line size in bytes (x86-64 / ARM64).
     */
    public static final int CACHE_LINE_BYTES = 64;

    /**
     * Cache line alignment bitmask (~63L).
     */
    private static final long CACHE_LINE_MASK = ~(CACHE_LINE_BYTES - 1L);

    /**
     * Header size in bytes, precisely aligned to 1 cache line (64 bytes).
     */
    public static final int HEADER_SIZE = CACHE_LINE_BYTES;

    /**
     * Aligns a 64-bit byte offset upwards to the next 64-byte boundary.
     *
     * @param offset the unaligned byte offset
     * @return the aligned byte offset >= offset
     */
    public static long align64(long offset) {
        return (offset + (CACHE_LINE_BYTES - 1L)) & CACHE_LINE_MASK;
    }

    /**
     * Aligns a 32-bit byte offset upwards to the next 64-byte boundary.
     *
     * @param offset the unaligned byte offset
     * @return the aligned byte offset >= offset
     */
    public static int align64(int offset) {
        return (offset + (CACHE_LINE_BYTES - 1)) & ~(CACHE_LINE_BYTES - 1);
    }

    /**
     * Checks if a 64-bit byte offset is evenly aligned to a 64-byte cache line boundary.
     *
     * @param offset the offset to inspect
     * @return true if aligned, false otherwise
     */
    public static boolean isAligned64(long offset) {
        return (offset & (CACHE_LINE_BYTES - 1L)) == 0L;
    }

    /**
     * Checks if a 32-bit byte offset is evenly aligned to a 64-byte cache line boundary.
     *
     * @param offset the offset to inspect
     * @return true if aligned, false otherwise
     */
    public static boolean isAligned64(int offset) {
        return (offset & (CACHE_LINE_BYTES - 1)) == 0;
    }

    /**
     * Computes the number of zero-padding bytes needed to reach the next 64-byte boundary.
     *
     * @param offset the current byte offset
     * @return number of padding bytes in range [0 .. 63]
     */
    public static int paddingBytesNeeded(long offset) {
        long remainder = offset & (CACHE_LINE_BYTES - 1L);
        return remainder == 0L ? 0 : (int) (CACHE_LINE_BYTES - remainder);
    }

    private VstmbCacheLineAligner() {
        // Alignment math utility
    }
}
