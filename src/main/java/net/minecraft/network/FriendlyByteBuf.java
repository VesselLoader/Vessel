package net.minecraft.network;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Minecraft network packet byte buffer ({@code net.minecraft.network.FriendlyByteBuf}) providing
 * VarInt, UTF-8 string, {@link ResourceLocation}, and {@link BlockPos} serialization.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class FriendlyByteBuf {

    private byte[] buffer;
    private int readerIndex;
    private int writerIndex;

    /**
     * Constructs a new {@code FriendlyByteBuf} with an initial 256-byte capacity.
     */
    public FriendlyByteBuf() {
        this.buffer = new byte[256];
        this.readerIndex = 0;
        this.writerIndex = 0;
    }

    /**
     * Constructs a {@code FriendlyByteBuf} wrapping a copy of the given byte array ready for
     * reading.
     *
     * @param initialBytes initial readable bytes
     */
    public FriendlyByteBuf(byte[] initialBytes) {
        Objects.requireNonNull(initialBytes, "initialBytes must not be null");
        this.buffer = Arrays.copyOf(initialBytes, Math.max(initialBytes.length, 64));
        this.readerIndex = 0;
        this.writerIndex = initialBytes.length;
    }

    /**
     * Writes a single byte to this buffer.
     *
     * @param value byte value (low 8 bits written)
     * @return {@code this} buffer
     */
    public FriendlyByteBuf writeByte(int value) {
        ensureWritable(1);
        buffer[writerIndex++] = (byte) value;
        return this;
    }

    /**
     * Reads a single byte from this buffer.
     *
     * @return read byte
     */
    public byte readByte() {
        ensureReadable(1);
        return buffer[readerIndex++];
    }

    /**
     * Writes a boolean value as a single byte ({@code 1} or {@code 0}).
     *
     * @param value boolean value
     * @return {@code this} buffer
     */
    public FriendlyByteBuf writeBoolean(boolean value) {
        return writeByte(value ? 1 : 0);
    }

    /**
     * Reads a boolean value from a single byte.
     *
     * @return read boolean
     */
    public boolean readBoolean() {
        return readByte() != 0;
    }

    /**
     * Writes a 32-bit big-endian integer.
     *
     * @param value integer value
     * @return {@code this} buffer
     */
    public FriendlyByteBuf writeInt(int value) {
        ensureWritable(4);
        buffer[writerIndex++] = (byte) (value >>> 24);
        buffer[writerIndex++] = (byte) (value >>> 16);
        buffer[writerIndex++] = (byte) (value >>> 8);
        buffer[writerIndex++] = (byte) value;
        return this;
    }

    /**
     * Reads a 32-bit big-endian integer.
     *
     * @return read integer
     */
    public int readInt() {
        ensureReadable(4);
        return ((buffer[readerIndex++] & 0xFF) << 24)
                | ((buffer[readerIndex++] & 0xFF) << 16)
                | ((buffer[readerIndex++] & 0xFF) << 8)
                | (buffer[readerIndex++] & 0xFF);
    }

    /**
     * Writes a variable-length 32-bit integer (VarInt).
     *
     * @param value integer value
     * @return {@code this} buffer
     */
    public FriendlyByteBuf writeVarInt(int value) {
        int current = value;
        while ((current & ~0x7F) != 0) {
            writeByte((current & 0x7F) | 0x80);
            current >>>= 7;
        }
        writeByte(current);
        return this;
    }

    /**
     * Reads a variable-length 32-bit integer (VarInt).
     *
     * @return decoded integer
     */
    public int readVarInt() {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = readByte();
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new IllegalStateException("VarInt is too big");
            }
        } while ((read & 0x80) != 0);
        return result;
    }

    /**
     * Writes a 64-bit big-endian long integer.
     *
     * @param value long value
     * @return {@code this} buffer
     */
    public FriendlyByteBuf writeLong(long value) {
        ensureWritable(8);
        buffer[writerIndex++] = (byte) (value >>> 56);
        buffer[writerIndex++] = (byte) (value >>> 48);
        buffer[writerIndex++] = (byte) (value >>> 40);
        buffer[writerIndex++] = (byte) (value >>> 32);
        buffer[writerIndex++] = (byte) (value >>> 24);
        buffer[writerIndex++] = (byte) (value >>> 16);
        buffer[writerIndex++] = (byte) (value >>> 8);
        buffer[writerIndex++] = (byte) value;
        return this;
    }

    /**
     * Reads a 64-bit big-endian long integer.
     *
     * @return read long
     */
    public long readLong() {
        ensureReadable(8);
        return ((long) (buffer[readerIndex++] & 0xFF) << 56)
                | ((long) (buffer[readerIndex++] & 0xFF) << 48)
                | ((long) (buffer[readerIndex++] & 0xFF) << 40)
                | ((long) (buffer[readerIndex++] & 0xFF) << 32)
                | ((long) (buffer[readerIndex++] & 0xFF) << 24)
                | ((long) (buffer[readerIndex++] & 0xFF) << 16)
                | ((long) (buffer[readerIndex++] & 0xFF) << 8)
                | ((long) (buffer[readerIndex++] & 0xFF));
    }

    /**
     * Writes a VarInt-prefixed UTF-8 string.
     *
     * @param value non-null string value
     * @return {@code this} buffer
     */
    public FriendlyByteBuf writeUtf(String value) {
        Objects.requireNonNull(value, "value must not be null");
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length);
        ensureWritable(bytes.length);
        System.arraycopy(bytes, 0, buffer, writerIndex, bytes.length);
        writerIndex += bytes.length;
        return this;
    }

    /**
     * Reads a VarInt-prefixed UTF-8 string.
     *
     * @return decoded string
     */
    public String readUtf() {
        int length = readVarInt();
        if (length < 0) {
            throw new IllegalStateException("Negative UTF string byte length: " + length);
        }
        ensureReadable(length);
        String str = new String(buffer, readerIndex, length, StandardCharsets.UTF_8);
        readerIndex += length;
        return str;
    }

    /**
     * Writes a {@link ResourceLocation} as a UTF-8 string.
     *
     * @param location non-null resource location
     * @return {@code this} buffer
     */
    public FriendlyByteBuf writeResourceLocation(ResourceLocation location) {
        Objects.requireNonNull(location, "location must not be null");
        return writeUtf(location.toString());
    }

    /**
     * Reads a {@link ResourceLocation} from a UTF-8 string.
     *
     * @return decoded {@link ResourceLocation}
     */
    public ResourceLocation readResourceLocation() {
        return ResourceLocation.parse(readUtf());
    }

    /**
     * Writes a {@link BlockPos} packed into a 64-bit long.
     *
     * @param pos non-null block position
     * @return {@code this} buffer
     */
    public FriendlyByteBuf writeBlockPos(BlockPos pos) {
        Objects.requireNonNull(pos, "pos must not be null");
        return writeLong(pos.asLong());
    }

    /**
     * Reads a {@link BlockPos} unpacked from a 64-bit long.
     *
     * @return decoded {@link BlockPos}
     */
    public BlockPos readBlockPos() {
        return BlockPos.of(readLong());
    }

    /**
     * Returns the number of unread bytes remaining between {@code readerIndex} and
     * {@code writerIndex}.
     *
     * @return readable byte count
     */
    public int readableBytes() {
        return writerIndex - readerIndex;
    }

    /**
     * Returns a copy of all bytes written to this buffer from index {@code 0} to
     * {@code writerIndex}.
     *
     * @return written byte array
     */
    public byte[] toByteArray() {
        return Arrays.copyOfRange(buffer, 0, writerIndex);
    }

    private void ensureWritable(int additionalBytes) {
        int required = writerIndex + additionalBytes;
        if (required > buffer.length) {
            int newCapacity = Math.max(buffer.length * 2, required);
            buffer = Arrays.copyOf(buffer, newCapacity);
        }
    }

    private void ensureReadable(int neededBytes) {
        if (readerIndex + neededBytes > writerIndex) {
            throw new IndexOutOfBoundsException(
                    "Not enough readable bytes: needed " + neededBytes
                            + ", available " + readableBytes());
        }
    }
}
