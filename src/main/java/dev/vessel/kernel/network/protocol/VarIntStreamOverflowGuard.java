package dev.vessel.kernel.network.protocol;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Fast, branch-reduced variable-length integer (VarInt and VarLong) encoder and decoder
 * equipped with strict byte stream overflow guards, preventing malicious DoS attacks
 * engineered to trigger infinite decoding loops or Netty channel pipeline crashes.
 *
 * <p>Responsibility: Guarded VarInt/VarLong streaming and overflow interception.
 * Eliminates: ERR-84, SEC-0201..0400.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VarIntStreamOverflowGuard {

    /**
     * Maximum permissible byte length of a standard 32-bit Minecraft VarInt (5 bytes).
     */
    public static final int MAX_VARINT_BYTES = 5;

    /**
     * Maximum permissible byte length of a 64-bit Minecraft VarLong (10 bytes).
     */
    public static final int MAX_VARLONG_BYTES = 10;

    /**
     * Bitmask extracting the 7 payload data bits of each byte (0x7F = 01111111b).
     */
    public static final int SEGMENT_BITS = 0x7F;

    /**
     * Bitmask testing the continuation flag bit (0x80 = 10000000b).
     */
    public static final int CONTINUE_BIT = 0x80;

    /**
     * Constructs a new VarIntStreamOverflowGuard.
     */
    public VarIntStreamOverflowGuard() {
    }

    /**
     * Decodes a 32-bit VarInt from the given {@link ByteBuffer}, strictly enforcing the 5-byte limit.
     *
     * @param buffer source byte buffer
     * @return decoded 32-bit integer
     * @throws VarIntOverflowException if the VarInt exceeds 5 bytes
     */
    public int readVarInt(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");

        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = buffer.get();
            value |= (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) {
                break;
            }

            position += 7;
            if (position >= 35) {
                throw new VarIntOverflowException(
                        "VarInt stream overflow! Exceeded maximum length of 5 bytes",
                        (position / 7) + 1,
                        MAX_VARINT_BYTES
                );
            }
        }

        return value;
    }

    /**
     * Decodes a 64-bit VarLong from the given {@link ByteBuffer}, strictly enforcing the 10-byte limit.
     *
     * @param buffer source byte buffer
     * @return decoded 64-bit long
     * @throws VarIntOverflowException if the VarLong exceeds 10 bytes
     */
    public long readVarLong(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");

        long value = 0L;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = buffer.get();
            value |= (long) (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) {
                break;
            }

            position += 7;
            if (position >= 70) {
                throw new VarIntOverflowException(
                        "VarLong stream overflow! Exceeded maximum length of 10 bytes",
                        (position / 7) + 1,
                        MAX_VARLONG_BYTES
                );
            }
        }

        return value;
    }

    /**
     * Decodes a 32-bit VarInt from a raw byte array starting at a specific offset.
     *
     * @param data   source byte array
     * @param offset initial array offset
     * @return decoded integer
     * @throws VarIntOverflowException if the VarInt exceeds 5 bytes
     */
    public int readVarIntFromArray(byte[] data, int offset) {
        Objects.requireNonNull(data, "data must not be null");
        if (offset < 0 || offset >= data.length) {
            throw new IndexOutOfBoundsException("Invalid offset: " + offset + " for array length: " + data.length);
        }

        int value = 0;
        int position = 0;
        int index = offset;

        while (true) {
            if (index >= data.length) {
                throw new IndexOutOfBoundsException("Unexpected end of byte array while reading VarInt at index: " + index);
            }

            byte currentByte = data[index++];
            value |= (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) {
                break;
            }

            position += 7;
            if (position >= 35) {
                throw new VarIntOverflowException(
                        "VarInt byte array overflow! Exceeded maximum length of 5 bytes",
                        (position / 7) + 1,
                        MAX_VARINT_BYTES
                );
            }
        }

        return value;
    }

    /**
     * Encodes a 32-bit integer into a VarInt and writes it to the destination {@link ByteBuffer}.
     *
     * @param value       integer value to encode
     * @param destination target ByteBuffer
     * @return number of bytes written (1 to 5)
     */
    public int writeVarInt(int value, ByteBuffer destination) {
        Objects.requireNonNull(destination, "destination must not be null");

        int bytesWritten = 0;
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                destination.put((byte) value);
                bytesWritten++;
                return bytesWritten;
            }

            destination.put((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));
            value >>>= 7;
            bytesWritten++;
        }
    }

    /**
     * Encodes a 64-bit long into a VarLong and writes it to the destination {@link ByteBuffer}.
     *
     * @param value       long value to encode
     * @param destination target ByteBuffer
     * @return number of bytes written (1 to 10)
     */
    public int writeVarLong(long value, ByteBuffer destination) {
        Objects.requireNonNull(destination, "destination must not be null");

        int bytesWritten = 0;
        while (true) {
            if ((value & ~((long) SEGMENT_BITS)) == 0L) {
                destination.put((byte) value);
                bytesWritten++;
                return bytesWritten;
            }

            destination.put((byte) ((value & SEGMENT_BITS) | CONTINUE_BIT));
            value >>>= 7;
            bytesWritten++;
        }
    }

    /**
     * Computes the serialized byte size of a 32-bit integer encoded as a VarInt without writing.
     *
     * @param value the integer value
     * @return required byte length (1 to 5)
     */
    public int computeVarIntSize(int value) {
        for (int i = 1; i < MAX_VARINT_BYTES; i++) {
            if ((value & (0xFFFFFFFF << (i * 7))) == 0) {
                return i;
            }
        }
        return MAX_VARINT_BYTES;
    }

    /**
     * Computes the serialized byte size of a 64-bit long encoded as a VarLong without writing.
     *
     * @param value the long value
     * @return required byte length (1 to 10)
     */
    public int computeVarLongSize(long value) {
        for (int i = 1; i < MAX_VARLONG_BYTES; i++) {
            if ((value & (0xFFFFFFFFFFFFFFFFL << (i * 7))) == 0L) {
                return i;
            }
        }
        return MAX_VARLONG_BYTES;
    }
}
