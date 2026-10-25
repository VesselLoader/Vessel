package dev.vessel.kernel.sanitizer;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Tracking container holding weak references and telemetry for an allocated direct buffer.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MonitoredDirectBuffer {

    private final String sourceContext;
    private final int capacity;
    private final WeakReference<ByteBuffer> bufferReference;
    private final long allocationEpochMs;

    public MonitoredDirectBuffer(String sourceContext, ByteBuffer buffer) {
        this.sourceContext = Objects.requireNonNull(sourceContext, "sourceContext must not be null");
        Objects.requireNonNull(buffer, "buffer must not be null");
        this.capacity = buffer.capacity();
        this.bufferReference = new WeakReference<>(buffer);
        this.allocationEpochMs = System.currentTimeMillis();
    }

    public String getSourceContext() {
        return sourceContext;
    }

    public int getCapacity() {
        return capacity;
    }

    public ByteBuffer getBuffer() {
        return bufferReference.get();
    }

    public boolean isOrphaned() {
        return bufferReference.get() == null;
    }

    public long getAllocationEpochMs() {
        return allocationEpochMs;
    }
}
