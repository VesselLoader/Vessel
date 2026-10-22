package dev.vessel.kernel.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Coalescing packet batcher for bundling outgoing network packets into unified TCP/RakNet socket frames.
 * <p>
 * Eliminates kernel socket syscall thrashing and network packet fragmentation by batching
 * small entity movement, particle, and metadata packets into a single network buffer.
 * </p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-6)
 */
public class NetworkPacketBatcher {

    private final List<ZeroCopyPacketFrame> pendingFrames = new ArrayList<>();

    /**
     * Enqueues a packet frame into the current tick outbound batch.
     *
     * @param frame packet frame to queue
     */
    public synchronized void queuePacket(ZeroCopyPacketFrame frame) {
        Objects.requireNonNull(frame, "frame cannot be null");
        pendingFrames.add(frame);
    }

    /**
     * Returns total number of pending frames in the queue.
     *
     * @return pending frame count
     */
    public synchronized int queuedCount() {
        return pendingFrames.size();
    }

    public synchronized boolean hasPendingPackets() {
        return !pendingFrames.isEmpty();
    }

    /**
     * Flushes and serializes all queued frames into a single coalesced byte buffer,
     * resetting the queue for the next tick.
     *
     * @return coalesced network payload bytes
     */
    public synchronized byte[] flushToBatch() {
        if (pendingFrames.isEmpty()) {
            return new byte[0];
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (ZeroCopyPacketFrame frame : pendingFrames) {
            // Write packet framing header: [PacketId (2 bytes)][Length (4 bytes)][Payload bytes]
            ByteBuffer header = ByteBuffer.allocate(6);
            header.putShort((short) frame.packetId());
            header.putInt(frame.payloadLength());
            baos.writeBytes(header.array());

            try {
                baos.write(frame.payload().toByteArrayCopy());
            } catch (IOException ignored) {}
        }

        pendingFrames.clear();
        return baos.toByteArray();
    }

    /**
     * Clears all pending frames without flushing.
     */
    public synchronized void clear() {
        pendingFrames.clear();
    }
}
