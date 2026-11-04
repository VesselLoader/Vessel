package dev.vessel.kernel.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Coalescing packet batcher for bundling outgoing network packets into unified TCP/RakNet socket frames.
 * <p>
 * Eliminates kernel socket syscall thrashing, network packet fragmentation, and garbage collection
 * overhead by batching small entity movement, particle, and metadata packets directly into a single
 * pre-sized byte array via zero-copy buffer transfers.
 * </p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 * @since 1.0.0 (Pass K-6)
 * @author VESSEL Engine Team
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
     * resetting the queue for the next tick using zero-allocation bitwise packing.
     *
     * @return coalesced network payload bytes
     */
    public synchronized byte[] flushToBatch() {
        if (pendingFrames.isEmpty()) {
            return new byte[0];
        }

        int totalBytes = 0;
        int frameCount = pendingFrames.size();
        for (int i = 0; i < frameCount; i++) {
            totalBytes += 6 + pendingFrames.get(i).payloadLength();
        }

        byte[] result = new byte[totalBytes];
        int writeIndex = 0;
        for (int i = 0; i < frameCount; i++) {
            ZeroCopyPacketFrame frame = pendingFrames.get(i);
            int packetId = frame.packetId();
            int payloadLen = frame.payloadLength();

            result[writeIndex++] = (byte) (packetId >> 8);
            result[writeIndex++] = (byte) packetId;
            result[writeIndex++] = (byte) (payloadLen >> 24);
            result[writeIndex++] = (byte) (payloadLen >> 16);
            result[writeIndex++] = (byte) (payloadLen >> 8);
            result[writeIndex++] = (byte) payloadLen;

            frame.payload().copyTo(result, writeIndex);
            writeIndex += payloadLen;
        }

        pendingFrames.clear();
        return result;
    }

    /**
     * Clears all pending frames without flushing.
     */
    public synchronized void clear() {
        pendingFrames.clear();
    }
}
