package dev.vessel.kernel.resourcepack.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/**
 * Zero-copy chunk streaming engine using Java NIO {@link FileChannel} and 64 KB chunk buffers.
 * <p>
 * Eliminates heap exhaustion and GC pauses caused by {@code Files.readAllBytes} when serving
 * massive multi-hundred megabyte modded resource packs. Streams data in contiguous 64 KB blocks
 * directly from disk channel to the HTTP client socket channel, supporting both full archive
 * transfers and precise byte-range resumes.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselFileChannelChunkStreamer {

    /** Default chunk buffer size: 64 KB (65,536 bytes). */
    public static final int DEFAULT_CHUNK_SIZE_BYTES = 64 * 1024;

    private final int chunkSize;

    /**
     * Constructs a chunk streamer with the default 64 KB buffer size.
     */
    public VesselFileChannelChunkStreamer() {
        this(DEFAULT_CHUNK_SIZE_BYTES);
    }

    /**
     * Constructs a chunk streamer with a custom buffer size.
     *
     * @param chunkSize chunk size in bytes
     */
    public VesselFileChannelChunkStreamer(int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive: " + chunkSize);
        }
        this.chunkSize = chunkSize;
    }

    /**
     * Streams the entire contents of a file channel to an output stream using 64 KB chunk buffers.
     *
     * @param fileChannel source NIO file channel
     * @param outputStream destination HTTP exchange output stream
     * @return total number of bytes streamed
     * @throws IOException on I/O or network socket errors
     */
    public long streamAll(FileChannel fileChannel, OutputStream outputStream) throws IOException {
        Objects.requireNonNull(fileChannel, "fileChannel cannot be null");
        Objects.requireNonNull(outputStream, "outputStream cannot be null");

        long fileSize = fileChannel.size();
        return streamRange(fileChannel, 0L, fileSize, outputStream);
    }

    /**
     * Streams a specific byte range from a file channel to an output stream using 64 KB chunk buffers.
     *
     * @param fileChannel source NIO file channel
     * @param startOffset inclusive starting byte offset
     * @param length number of bytes to stream
     * @param outputStream destination HTTP exchange output stream
     * @return total number of bytes streamed
     * @throws IOException on I/O or network socket errors
     */
    public long streamRange(FileChannel fileChannel, long startOffset, long length, OutputStream outputStream) throws IOException {
        Objects.requireNonNull(fileChannel, "fileChannel cannot be null");
        Objects.requireNonNull(outputStream, "outputStream cannot be null");

        if (startOffset < 0) {
            throw new IllegalArgumentException("startOffset cannot be negative: " + startOffset);
        }
        if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative: " + length);
        }
        if (length == 0) {
            outputStream.flush();
            return 0L;
        }

        fileChannel.position(startOffset);
        WritableByteChannel targetChannel = Channels.newChannel(outputStream);
        ByteBuffer buffer = ByteBuffer.allocate(Math.min(chunkSize, (int) Math.min(length, Integer.MAX_VALUE)));

        long bytesRemaining = length;
        long totalStreamed = 0L;

        while (bytesRemaining > 0) {
            int toRead = (int) Math.min(buffer.capacity(), bytesRemaining);
            buffer.limit(toRead);
            buffer.position(0);

            int bytesRead = fileChannel.read(buffer);
            if (bytesRead < 0) {
                break; // Unexpected early EOF on underlying file
            }

            buffer.flip();
            while (buffer.hasRemaining()) {
                targetChannel.write(buffer);
            }

            totalStreamed += bytesRead;
            bytesRemaining -= bytesRead;
        }

        outputStream.flush();
        return totalStreamed;
    }

    /**
     * Returns the configured buffer chunk size in bytes.
     *
     * @return chunk size in bytes
     */
    public int getChunkSize() {
        return chunkSize;
    }
}
