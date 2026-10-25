package dev.vessel.kernel.optimization.network;

/**
 * Krypton & ImmediatelyFast-inspired zero-allocation VarInt serializer/deserializer.
 * <p>
 * In Minecraft networking protocols (Java Edition and Bedrock RakNet multiplexing),
 * packet lengths, IDs, and numeric tokens are encoded as variable-length quantities (VarInt).
 * Traditional implementations allocate intermediate buffers or invoke virtual stream calls per byte.
 * This codec performs direct bitwise manipulation on raw byte arrays with zero object allocation.
 *
 * @version 1.0.0
 */
public final class FastVarIntCodec {

    private FastVarIntCodec() {
        // Utility class
    }

    /**
     * Calculates the number of bytes required to serialize an integer as a VarInt.
     *
     * @param value the integer value
     * @return size in bytes (1 to 5)
     */
    public static int getVarIntSize(int value) {
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            return 1;
        }
        if ((value & (0xFFFFFFFF << 14)) == 0) {
            return 2;
        }
        if ((value & (0xFFFFFFFF << 21)) == 0) {
            return 3;
        }
        if ((value & (0xFFFFFFFF << 28)) == 0) {
            return 4;
        }
        return 5;
    }

    /**
     * Serializes an integer as a VarInt into the destination byte array starting at the given offset.
     *
     * @param value  integer to serialize
     * @param target destination byte array
     * @param offset start offset
     * @return number of bytes written
     */
    public static int writeVarInt(int value, byte[] target, int offset) {
        int pos = offset;
        while (true) {
            if ((value & ~0x7F) == 0) {
                target[pos++] = (byte) value;
                return pos - offset;
            }
            target[pos++] = (byte) ((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    /**
     * Deserializes a VarInt from the source byte array starting at the given offset.
     *
     * @param source source byte array
     * @param offset start offset
     * @return a packed 64-bit value where the lower 32 bits are the decoded int value
     *         and the upper 32 bits are the number of bytes read
     */
    public static long readVarIntPacked(byte[] source, int offset) {
        int value = 0;
        int bytesRead = 0;
        byte b;
        do {
            if (bytesRead >= 5) {
                throw new IllegalArgumentException("VarInt is too big (> 5 bytes)");
            }
            if (offset + bytesRead >= source.length) {
                throw new IndexOutOfBoundsException("Unexpected end of byte array reading VarInt");
            }
            b = source[offset + bytesRead];
            value |= (b & 0x7F) << (bytesRead * 7);
            bytesRead++;
        } while ((b & 0x80) != 0);

        return (((long) bytesRead) << 32) | (value & 0xFFFFFFFFL);
    }

    /**
     * Extracts the decoded integer value from the packed result of {@link #readVarIntPacked(byte[], int)}.
     *
     * @param packed packed 64-bit result
     * @return decoded int
     */
    public static int unpackValue(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }

    /**
     * Extracts the number of bytes consumed from the packed result of {@link #readVarIntPacked(byte[], int)}.
     *
     * @param packed packed 64-bit result
     * @return bytes consumed
     */
    public static int unpackBytesRead(long packed) {
        return (int) (packed >>> 32);
    }
}
