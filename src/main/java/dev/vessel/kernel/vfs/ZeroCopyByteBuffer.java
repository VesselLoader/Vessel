package dev.vessel.kernel.vfs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Immutable zero-copy byte buffer for high-throughput VFS and bytecode operations.
 * <p>
 * Eliminates defensive array allocations and GC churn during classloading and resource queries
 * by providing safe, read-only window views over byte sequences.
 * </p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-5)
 */
public final class ZeroCopyByteBuffer {

    private final byte[] array;
    private final int offset;
    private final int length;

    private ZeroCopyByteBuffer(byte[] array, int offset, int length) {
        this.array = Objects.requireNonNull(array, "array cannot be null");
        if (offset < 0 || length < 0 || offset + length > array.length) {
            throw new IndexOutOfBoundsException(
                    String.format("offset=%d, length=%d, array.length=%d", offset, length, array.length)
            );
        }
        this.offset = offset;
        this.length = length;
    }

    /**
     * Wraps an entire byte array without copying.
     *
     * @param array source byte array
     * @return immutable zero-copy buffer
     */
    public static ZeroCopyByteBuffer wrap(byte[] array) {
        Objects.requireNonNull(array, "array cannot be null");
        return new ZeroCopyByteBuffer(array, 0, array.length);
    }

    /**
     * Wraps a slice of a byte array without copying.
     *
     * @param array source byte array
     * @param offset start offset
     * @param length slice length
     * @return immutable zero-copy buffer
     */
    public static ZeroCopyByteBuffer wrap(byte[] array, int offset, int length) {
        return new ZeroCopyByteBuffer(array, offset, length);
    }

    /**
     * Returns total byte length of this buffer.
     *
     * @return buffer length
     */
    public int length() {
        return length;
    }

    /**
     * Retrieves byte at relative buffer index.
     *
     * @param index zero-based index
     * @return byte value
     */
    public byte get(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
        }
        return array[offset + index];
    }

    /**
     * Returns a detached, defensive copy of the underlying bytes.
     * Should only be called when external consumers strictly require mutable arrays.
     *
     * @return new byte array containing buffer content
     */
    public byte[] toByteArrayCopy() {
        byte[] copy = new byte[length];
        System.arraycopy(array, offset, copy, 0, length);
        return copy;
    }

    /**
     * Creates an InputStream reading directly from this buffer without memory allocation.
     *
     * @return ByteArrayInputStream viewing the buffer
     */
    public InputStream asInputStream() {
        return new ByteArrayInputStream(array, offset, length);
    }

    /**
     * Creates a read-only NIO ByteBuffer view over this buffer.
     *
     * @return read-only ByteBuffer
     */
    public ByteBuffer asReadOnlyByteBuffer() {
        return ByteBuffer.wrap(array, offset, length).asReadOnlyBuffer();
    }
}
