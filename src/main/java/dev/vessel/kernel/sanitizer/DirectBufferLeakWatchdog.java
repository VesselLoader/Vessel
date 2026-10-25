package dev.vessel.kernel.sanitizer;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Watchdog actively monitoring off-heap direct byte buffer allocations, discovering orphaned buffers,
 * and performing explicit native deallocation to prevent OutOfMemoryError in high-throughput network arenas.
 *
 * <p>Remediates ERR-47 and JVM-0201..0350 by eliminating uncollected direct memory leaks.</p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DirectBufferLeakWatchdog {

    public static final long DEFAULT_MAX_DIRECT_MEMORY_BYTES = 512L * 1024L * 1024L; // 512 MB threshold

    private final List<MonitoredDirectBuffer> trackedBuffers = new CopyOnWriteArrayList<>();
    private final AtomicLong totalAllocatedDirectBytes = new AtomicLong(0);
    private final AtomicLong totalReclaimedDirectBytes = new AtomicLong(0);

    public DirectBufferLeakWatchdog() {
    }

    /**
     * Registers a direct ByteBuffer to be guarded against orphan leaks.
     *
     * @param sourceContext diagnostic tag or module origin
     * @param buffer direct byte buffer to track
     */
    public void trackDirectBuffer(String sourceContext, ByteBuffer buffer) {
        Objects.requireNonNull(sourceContext, "sourceContext must not be null");
        Objects.requireNonNull(buffer, "buffer must not be null");

        if (!buffer.isDirect()) {
            return;
        }

        trackedBuffers.add(new MonitoredDirectBuffer(sourceContext, buffer));
        totalAllocatedDirectBytes.addAndGet(buffer.capacity());
    }

    /**
     * Explicitly releases and unregisters a direct byte buffer.
     *
     * @param buffer buffer to release immediately
     * @return {@code true} if deallocation was performed
     */
    public boolean releaseDirectBuffer(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "buffer must not be null");
        boolean freed = DirectBufferDeallocator.deallocate(buffer);

        Iterator<MonitoredDirectBuffer> iterator = trackedBuffers.iterator();
        while (iterator.hasNext()) {
            MonitoredDirectBuffer monitored = iterator.next();
            ByteBuffer ref = monitored.getBuffer();
            if (ref == buffer || ref == null) {
                trackedBuffers.remove(monitored);
                totalReclaimedDirectBytes.addAndGet(monitored.getCapacity());
                break;
            }
        }
        return freed;
    }

    /**
     * Returns the count of actively tracked buffer entries.
     *
     * @return tracked buffer count
     */
    public int getTrackedCount() {
        return trackedBuffers.size();
    }

    /**
     * Scans for orphaned or stale direct buffers and executes reclamation passes.
     *
     * @return audit and reclamation report
     */
    public DirectBufferAuditReport auditAndReclaim() {
        long startNanos = System.nanoTime();
        int orphanedCount = 0;
        long reclaimedBytes = 0;

        Iterator<MonitoredDirectBuffer> iterator = trackedBuffers.iterator();
        while (iterator.hasNext()) {
            MonitoredDirectBuffer entry = iterator.next();
            if (entry.isOrphaned()) {
                orphanedCount++;
                reclaimedBytes += entry.getCapacity();
                trackedBuffers.remove(entry);
                totalReclaimedDirectBytes.addAndGet(entry.getCapacity());
            }
        }

        long currentAllocated = Math.max(0, totalAllocatedDirectBytes.get() - totalReclaimedDirectBytes.get());
        long elapsedNanos = System.nanoTime() - startNanos;

        return new DirectBufferAuditReport(
                trackedBuffers.size(),
                orphanedCount,
                reclaimedBytes,
                currentAllocated,
                elapsedNanos
        );
    }
}
