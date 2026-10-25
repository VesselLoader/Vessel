package dev.vessel.adapter.bridge;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Guards against stack-overflow neighbor/tick update recursion caused by modded tick
 * accelerators (Torcherino, ProjectE Watch of Flowing Time, SolarFlux, Industrial Foregoing).
 *
 * @version 1.0.0
 */
public final class BlockUpdateRecursionGuard {

    private final AtomicInteger currentDepth = new AtomicInteger(0);

    /**
     * Creates a new block update recursion guard.
     */
    public BlockUpdateRecursionGuard() {
    }

    /**
     * Attempts to enter a nested block update at {@code packedBlockPos}, rejecting the
     * update if the current recursion depth has reached {@code maxRecursionDepth}.
     *
     * @param packedBlockPos    64-bit packed block position
     * @param maxRecursionDepth maximum allowed recursion depth (e.g., 64)
     * @return {@code true} if the block update is permitted, {@code false} if throttled
     */
    public boolean enterBlockUpdate(long packedBlockPos, int maxRecursionDepth) {
        while (true) {
            int depth = currentDepth.get();
            if (depth >= maxRecursionDepth) {
                return false;
            }
            if (currentDepth.compareAndSet(depth, depth + 1)) {
                return true;
            }
        }
    }

    /**
     * Exits a previously entered block update at {@code packedBlockPos}, decrementing
     * the active recursion depth counter.
     *
     * @param packedBlockPos 64-bit packed block position
     */
    public void exitBlockUpdate(long packedBlockPos) {
        currentDepth.updateAndGet(depth -> Math.max(0, depth - 1));
    }

    /**
     * Returns the current block update recursion depth.
     *
     * @return current recursion depth
     */
    public int getCurrentDepth() {
        return currentDepth.get();
    }
}
