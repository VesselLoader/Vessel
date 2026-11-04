package dev.vessel.kernel.tick;

/**
 * Thread-local container providing isolated {@link FlatArrayMinHeapPathfinder} instances per worker thread.
 * Eliminates synchronization overhead and cross-thread state corruption in multi-threaded Folia regions.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class PathfinderThreadLocal extends ThreadLocal<FlatArrayMinHeapPathfinder> {

    public PathfinderThreadLocal() {
    }

    @Override
    protected FlatArrayMinHeapPathfinder initialValue() {
        return new FlatArrayMinHeapPathfinder();
    }
}
