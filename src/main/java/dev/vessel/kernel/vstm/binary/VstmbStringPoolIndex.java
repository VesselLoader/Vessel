package dev.vessel.kernel.vstm.binary;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * High-performance string pool index for .vstmb binary files.
 *
 * <p>Enables $O(1)$ random access to UTF-8 JVM class, method, and field identifier strings
 * directly from memory-mapped off-heap storage. Contains both high-level string decoders
 * and zero-allocation primitive byte accessors (Catalog refs: MAP-0112, ZER-0010).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmbStringPoolIndex {

    /**
     * Reads the total number of string entries recorded in the string pool.
     *
     * @param segment        the memory segment
     * @param poolBaseOffset the byte offset where the string pool begins
     * @return the number of strings in the pool
     */
    public static int getStringCount(MemorySegment segment, long poolBaseOffset) {
        return VstmbOffHeapByteReader.readInt(segment, poolBaseOffset);
    }

    /**
     * Retrieves the byte offset where the string data buffer begins.
     *
     * @param poolBaseOffset the start of the string pool
     * @param stringCount    the number of strings
     * @return the aligned start of the UTF-8 payload data
     */
    public static long getDataBaseOffset(long poolBaseOffset, int stringCount) {
        long rawOffset = poolBaseOffset + 4L + ((long) stringCount * 4L);
        return VstmbCacheLineAligner.align64(rawOffset);
    }

    /**
     * Reads a UTF-8 string by index from the memory-mapped string pool.
     *
     * @param segment        the memory segment
     * @param poolBaseOffset the start of the string pool
     * @param stringIndex    the zero-based string index
     * @return the decoded Java String
     */
    public static String getString(MemorySegment segment, long poolBaseOffset, int stringIndex) {
        int count = getStringCount(segment, poolBaseOffset);
        if (stringIndex < 0 || stringIndex >= count) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-STR-INDEX",
                    "String pool index out of bounds: " + stringIndex + " (total: " + count + ")"
            );
        }

        long indexTableOffset = poolBaseOffset + 4L + ((long) stringIndex * 4L);
        int relativeOffset = VstmbOffHeapByteReader.readInt(segment, indexTableOffset);

        long dataBase = getDataBaseOffset(poolBaseOffset, count);
        long targetOffset = dataBase + (long) relativeOffset;

        int length = Short.toUnsignedInt(VstmbOffHeapByteReader.readShort(segment, targetOffset));
        if (length == 0) {
            return "";
        }

        byte[] buffer = new byte[length];
        VstmbOffHeapByteReader.readBytes(segment, targetOffset + 2L, buffer, 0, length);
        return new String(buffer, StandardCharsets.UTF_8);
    }

    /**
     * Reads raw UTF-8 bytes for a string index directly into a pre-allocated destination buffer
     * without generating any garbage collection allocations.
     *
     * @param segment        the memory segment
     * @param poolBaseOffset the start of the string pool
     * @param stringIndex    the zero-based index
     * @param destination    the pre-allocated destination byte buffer
     * @param destOffset     the destination offset
     * @return the number of bytes written
     */
    public static int readUtf8Bytes(MemorySegment segment, long poolBaseOffset, int stringIndex, byte[] destination, int destOffset) {
        Objects.requireNonNull(destination, "destination must not be null");
        int count = getStringCount(segment, poolBaseOffset);
        if (stringIndex < 0 || stringIndex >= count) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-STR-INDEX",
                    "String pool index out of bounds: " + stringIndex + " (total: " + count + ")"
            );
        }

        long indexTableOffset = poolBaseOffset + 4L + ((long) stringIndex * 4L);
        int relativeOffset = VstmbOffHeapByteReader.readInt(segment, indexTableOffset);

        long dataBase = getDataBaseOffset(poolBaseOffset, count);
        long targetOffset = dataBase + (long) relativeOffset;

        int length = Short.toUnsignedInt(VstmbOffHeapByteReader.readShort(segment, targetOffset));
        if (length == 0) {
            return 0;
        }

        VstmbOffHeapByteReader.readBytes(segment, targetOffset + 2L, destination, destOffset, length);
        return length;
    }

    private VstmbStringPoolIndex() {
        // Static string pool access helper
    }
}
