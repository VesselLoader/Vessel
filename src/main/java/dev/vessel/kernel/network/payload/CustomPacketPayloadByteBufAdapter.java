package dev.vessel.kernel.network.payload;

import java.util.Arrays;
import java.util.Objects;

/**
 * Zero-copy memory buffer adapter for custom network payloads.
 *
 * <p>Wraps raw byte data without allocating intermediary Netty heap buffers,
 * enabling direct pass-through to Bedrock RakNet translation and Java packet pipelines.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class CustomPacketPayloadByteBufAdapter {

    private final byte[] buffer;
    private int readPosition;

    /**
     * Constructs a buffer adapter wrapping the supplied byte array.
     *
     * @param buffer byte array to wrap
     */
    public CustomPacketPayloadByteBufAdapter(byte[] buffer) {
        this.buffer = Objects.requireNonNull(buffer, "buffer cannot be null");
        this.readPosition = 0;
    }

    /**
     * Reads a 32-bit big-endian integer from the buffer.
     *
     * @return integer value
     */
    public int readInt() {
        if (readPosition + 4 > buffer.length) {
            return 0;
        }
        int val = ((buffer[readPosition] & 0xFF) << 24)
                | ((buffer[readPosition + 1] & 0xFF) << 16)
                | ((buffer[readPosition + 2] & 0xFF) << 8)
                | (buffer[readPosition + 3] & 0xFF);
        readPosition += 4;
        return val;
    }

    /**
     * Reads a UTF-8 string prefixed with a 16-bit short length.
     *
     * @return string value
     */
    public String readUtf() {
        if (readPosition + 2 > buffer.length) {
            return "";
        }
        int len = ((buffer[readPosition] & 0xFF) << 8) | (buffer[readPosition + 1] & 0xFF);
        readPosition += 2;
        if (readPosition + len > buffer.length) {
            return "";
        }
        String str = new String(buffer, readPosition, len, java.nio.charset.StandardCharsets.UTF_8);
        readPosition += len;
        return str;
    }

    /**
     * Resolves the backing byte array.
     *
     * @return raw bytes
     */
    public byte[] getRawBytes() {
        return buffer;
    }

    /**
     * Resolves the remaining unread bytes count.
     *
     * @return remaining bytes
     */
    public int readableBytes() {
        return Math.max(0, buffer.length - readPosition);
    }
}
