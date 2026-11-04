package dev.vessel.kernel.tick;

/**
 * Global pool providing thread-confined {@link FlatArrayMinHeapPathfinder} instances to Regional Folia threads.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ThreadLocalPathfinderPool {

    private static final PathfinderThreadLocal HOLDER = new PathfinderThreadLocal();

    private ThreadLocalPathfinderPool() {
    }

    /**
     * Obtains the thread-confined pathfinder instance for the current execution strand.
     *
     * @return active thread-confined FlatArrayMinHeapPathfinder
     */
    public static FlatArrayMinHeapPathfinder getThreadLocalPathfinder() {
        return HOLDER.get();
    }
}
