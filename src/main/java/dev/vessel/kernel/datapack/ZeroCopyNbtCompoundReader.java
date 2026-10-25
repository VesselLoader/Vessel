package dev.vessel.kernel.datapack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Zero-copy reader for NBT Compound payloads directly operating on {@link ByteBuffer}.
 * <p>
 * Eliminates ERR-98 and OPT-050..OPT-060 (transient NBT heap allocations, deep copy pauses,
 * and GC thrashing during chunk streaming and packet handling).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ZeroCopyNbtCompoundReader {

    public static final byte ID_END = 0;
    public static final byte ID_BYTE = 1;
    public static final byte ID_SHORT = 2;
    public static final byte ID_INT = 3;
    public static final byte ID_LONG = 4;
    public static final byte ID_FLOAT = 5;
    public static final byte ID_DOUBLE = 6;
    public static final byte ID_BYTE_ARRAY = 7;
    public static final byte ID_STRING = 8;
    public static final byte ID_LIST = 9;
    public static final byte ID_COMPOUND = 10;
    public static final byte ID_INT_ARRAY = 11;
    public static final byte ID_LONG_ARRAY = 12;

    private ZeroCopyNbtCompoundReader() {
        // Utility constructor
    }

    /**
     * Checks if a specified key exists at the root of this Compound without heap allocations.
     *
     * @param buffer source buffer (position preserved via duplicate)
     * @param key target tag name
     * @return true if key is present
     */
    public static boolean hasKey(ByteBuffer buffer, String key) {
        return findTagOffset(buffer, key) != -1;
    }

    /**
     * Reads an integer value directly from the compound without parsing other tags.
     *
     * @param buffer source buffer
     * @param key tag name
     * @param defaultValue fallback value
     * @return int value or defaultValue
     */
    public static int getInt(ByteBuffer buffer, String key, int defaultValue) {
        int payloadOffset = findTagOffset(buffer, key, ID_INT);
        if (payloadOffset == -1) {
            return defaultValue;
        }
        return buffer.getInt(payloadOffset);
    }

    /**
     * Reads a long value directly from the compound.
     *
     * @param buffer source buffer
     * @param key tag name
     * @param defaultValue fallback value
     * @return long value or defaultValue
     */
    public static long getLong(ByteBuffer buffer, String key, long defaultValue) {
        int payloadOffset = findTagOffset(buffer, key, ID_LONG);
        if (payloadOffset == -1) {
            return defaultValue;
        }
        return buffer.getLong(payloadOffset);
    }

    /**
     * Reads a byte value directly from the compound.
     *
     * @param buffer source buffer
     * @param key tag name
     * @param defaultValue fallback value
     * @return byte value or defaultValue
     */
    public static byte getByte(ByteBuffer buffer, String key, byte defaultValue) {
        int payloadOffset = findTagOffset(buffer, key, ID_BYTE);
        if (payloadOffset == -1) {
            return defaultValue;
        }
        return buffer.get(payloadOffset);
    }

    /**
     * Reads a boolean value (represented as byte) directly from the compound.
     *
     * @param buffer source buffer
     * @param key tag name
     * @param defaultValue fallback value
     * @return boolean value or defaultValue
     */
    public static boolean getBoolean(ByteBuffer buffer, String key, boolean defaultValue) {
        int payloadOffset = findTagOffset(buffer, key, ID_BYTE);
        if (payloadOffset == -1) {
            return defaultValue;
        }
        return buffer.get(payloadOffset) != 0;
    }

    /**
     * Reads a short value directly from the compound.
     *
     * @param buffer source buffer
     * @param key tag name
     * @param defaultValue fallback value
     * @return short value or defaultValue
     */
    public static short getShort(ByteBuffer buffer, String key, short defaultValue) {
        int payloadOffset = findTagOffset(buffer, key, ID_SHORT);
        if (payloadOffset == -1) {
            return defaultValue;
        }
        return buffer.getShort(payloadOffset);
    }

    /**
     * Reads a float value directly from the compound.
     *
     * @param buffer source buffer
     * @param key tag name
     * @param defaultValue fallback value
     * @return float value or defaultValue
     */
    public static float getFloat(ByteBuffer buffer, String key, float defaultValue) {
        int payloadOffset = findTagOffset(buffer, key, ID_FLOAT);
        if (payloadOffset == -1) {
            return defaultValue;
        }
        return buffer.getFloat(payloadOffset);
    }

    /**
     * Reads a double value directly from the compound.
     *
     * @param buffer source buffer
     * @param key tag name
     * @param defaultValue fallback value
     * @return double value or defaultValue
     */
    public static double getDouble(ByteBuffer buffer, String key, double defaultValue) {
        int payloadOffset = findTagOffset(buffer, key, ID_DOUBLE);
        if (payloadOffset == -1) {
            return defaultValue;
        }
        return buffer.getDouble(payloadOffset);
    }

    /**
     * Reads a UTF-8 string value from the compound.
     *
     * @param buffer source buffer
     * @param key tag name
     * @param defaultValue fallback value
     * @return string value or defaultValue
     */
    public static String getString(ByteBuffer buffer, String key, String defaultValue) {
        int payloadOffset = findTagOffset(buffer, key, ID_STRING);
        if (payloadOffset == -1) {
            return defaultValue;
        }
        int len = buffer.getShort(payloadOffset) & 0xFFFF;
        byte[] bytes = new byte[len];
        ByteBuffer dup = buffer.duplicate();
        dup.position(payloadOffset + 2);
        dup.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Finds the sub-region slice for a nested tag.
     *
     * @param buffer source buffer
     * @param key tag name
     * @return slice descriptor or null if not found
     */
    public static ZeroCopyNbtSlice findSlice(ByteBuffer buffer, String key) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        Objects.requireNonNull(key, "key cannot be null");

        ByteBuffer dup = buffer.duplicate();
        while (dup.hasRemaining()) {
            byte tagId = dup.get();
            if (tagId == ID_END) {
                break;
            }

            int nameLen = dup.getShort() & 0xFFFF;
            int namePos = dup.position();
            boolean match = matchesKey(dup, namePos, nameLen, key);
            dup.position(namePos + nameLen);

            int payloadPos = dup.position();
            int payloadLen = measurePayloadLength(dup, tagId);

            if (match) {
                return new ZeroCopyNbtSlice(payloadPos, payloadLen, NbtTagType.fromId(tagId));
            }
            dup.position(payloadPos + payloadLen);
        }
        return null;
    }

    /**
     * Finds the byte offset for the payload of a specific tag by key.
     *
     * @param buffer source buffer
     * @param key tag name
     * @return byte offset or -1 if not found
     */
    public static int findTagOffset(ByteBuffer buffer, String key) {
        return findTagOffset(buffer, key, (byte) -1);
    }

    /**
     * Finds the byte offset for the payload of a specific tag by key and expected type.
     *
     * @param buffer source buffer
     * @param key tag name
     * @param expectedType expected tag ID, or -1 for any type
     * @return byte offset or -1 if not found
     */
    public static int findTagOffset(ByteBuffer buffer, String key, byte expectedType) {
        Objects.requireNonNull(buffer, "buffer cannot be null");
        Objects.requireNonNull(key, "key cannot be null");

        ByteBuffer dup = buffer.duplicate();
        while (dup.hasRemaining()) {
            byte tagId = dup.get();
            if (tagId == ID_END) {
                break;
            }

            int nameLen = dup.getShort() & 0xFFFF;
            int namePos = dup.position();
            boolean match = matchesKey(dup, namePos, nameLen, key);
            dup.position(namePos + nameLen);

            int payloadPos = dup.position();
            if (match && (expectedType == -1 || tagId == expectedType)) {
                return payloadPos;
            }

            int payloadLen = measurePayloadLength(dup, tagId);
            dup.position(payloadPos + payloadLen);
        }
        return -1;
    }

    private static boolean matchesKey(ByteBuffer buf, int pos, int len, String key) {
        if (len != key.length()) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (buf.get(pos + i) != (byte) key.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static int measurePayloadLength(ByteBuffer buf, byte tagId) {
        return switch (tagId) {
            case ID_BYTE -> 1;
            case ID_SHORT -> 2;
            case ID_INT, ID_FLOAT -> 4;
            case ID_LONG, ID_DOUBLE -> 8;
            case ID_BYTE_ARRAY -> 4 + buf.getInt(buf.position());
            case ID_STRING -> 2 + (buf.getShort(buf.position()) & 0xFFFF);
            case ID_INT_ARRAY -> 4 + (buf.getInt(buf.position()) * 4);
            case ID_LONG_ARRAY -> 4 + (buf.getInt(buf.position()) * 8);
            case ID_LIST -> {
                int startPos = buf.position();
                byte elemType = buf.get();
                int count = buf.getInt();
                for (int i = 0; i < count; i++) {
                    int elemLen = measurePayloadLength(buf, elemType);
                    buf.position(buf.position() + elemLen);
                }
                int totalLen = buf.position() - startPos;
                buf.position(startPos);
                yield totalLen;
            }
            case ID_COMPOUND -> {
                int startPos = buf.position();
                while (buf.hasRemaining()) {
                    byte innerId = buf.get();
                    if (innerId == ID_END) {
                        break;
                    }
                    int nameLen = buf.getShort() & 0xFFFF;
                    buf.position(buf.position() + nameLen);
                    int innerPayloadLen = measurePayloadLength(buf, innerId);
                    buf.position(buf.position() + innerPayloadLen);
                }
                int totalLen = buf.position() - startPos;
                buf.position(startPos);
                yield totalLen;
            }
            default -> 0;
        };
    }
}
