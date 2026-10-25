package dev.vessel.kernel.vstm.binary;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * High-performance off-heap reader extracting primitive types directly from a {@link MemorySegment}.
 *
 * <p>Operates entirely outside the Java Virtual Machine GC heap with zero-allocation,
 * utilizing standard big-endian unaligned value layouts. Enforces strict safety boundary checks
 * to prevent segmentation faults and buffer over-reads (Catalog refs: ZER-0010, DVI-00142).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmbOffHeapByteReader {

    public static final ValueLayout.OfShort SHORT_LAYOUT = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    public static final ValueLayout.OfInt INT_LAYOUT = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    public static final ValueLayout.OfLong LONG_LAYOUT = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    /**
     * Reads a signed 8-bit byte at the specified off-heap offset.
     *
     * @param segment the memory segment
     * @param offset  the byte offset
     * @return the byte value
     */
    public static byte readByte(MemorySegment segment, long offset) {
        checkBounds(segment, offset, Byte.BYTES);
        return segment.get(ValueLayout.JAVA_BYTE, offset);
    }

    /**
     * Reads a signed 16-bit short in big-endian byte order at the specified offset.
     *
     * @param segment the memory segment
     * @param offset  the byte offset
     * @return the short value
     */
    public static short readShort(MemorySegment segment, long offset) {
        checkBounds(segment, offset, Short.BYTES);
        return segment.get(SHORT_LAYOUT, offset);
    }

    /**
     * Reads a signed 32-bit integer in big-endian byte order at the specified offset.
     *
     * @param segment the memory segment
     * @param offset  the byte offset
     * @return the integer value
     */
    public static int readInt(MemorySegment segment, long offset) {
        checkBounds(segment, offset, Integer.BYTES);
        return segment.get(INT_LAYOUT, offset);
    }

    /**
     * Reads a signed 64-bit long in big-endian byte order at the specified offset.
     *
     * @param segment the memory segment
     * @param offset  the byte offset
     * @return the long value
     */
    public static long readLong(MemorySegment segment, long offset) {
        checkBounds(segment, offset, Long.BYTES);
        return segment.get(LONG_LAYOUT, offset);
    }

    /**
     * Copies a slice of bytes from off-heap memory into a destination heap byte array.
     *
     * @param segment     the source memory segment
     * @param srcOffset   the source byte offset in off-heap memory
     * @param destination the destination byte array
     * @param destOffset  the starting offset in destination
     * @param length      the number of bytes to copy
     */
    public static void readBytes(MemorySegment segment, long srcOffset, byte[] destination, int destOffset, int length) {
        Objects.requireNonNull(destination, "destination buffer must not be null");
        if (length < 0 || destOffset < 0 || destOffset + length > destination.length) {
            throw new IndexOutOfBoundsException("Invalid destination range: destOffset=" + destOffset + ", length=" + length);
        }
        if (length == 0) {
            return;
        }
        checkBounds(segment, srcOffset, length);
        MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, srcOffset, destination, destOffset, length);
    }

    /**
     * Verifies that the access slice [offset .. offset + byteCount] is within segment bounds.
     *
     * @param segment   the memory segment
     * @param offset    the starting byte offset
     * @param byteCount the number of bytes to access
     */
    public static void checkBounds(MemorySegment segment, long offset, long byteCount) {
        Objects.requireNonNull(segment, "segment must not be null");
        if (offset < 0) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-BOUNDS",
                    "Negative offset in memory-mapped read: " + offset
            );
        }
        long segmentSize = segment.byteSize();
        if (offset + byteCount > segmentSize) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-BOUNDS",
                    "Off-heap read bounds violation: offset=" + offset
                            + ", bytes=" + byteCount
                            + ", segmentSize=" + segmentSize
            );
        }
    }

    private VstmbOffHeapByteReader() {
        // Static memory access facade
    }
}
