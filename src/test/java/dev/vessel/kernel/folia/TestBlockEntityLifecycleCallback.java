package dev.vessel.kernel.folia;

import dev.vessel.kernel.folia.blockentity.BlockEntityLifecycleCallback;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test implementation of {@link BlockEntityLifecycleCallback}.
 * <p>
 * Ensures zero anonymous classes are used in unit test suites (Rule 17).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TestBlockEntityLifecycleCallback implements BlockEntityLifecycleCallback {

    private final AtomicInteger detachCount = new AtomicInteger(0);
    private final AtomicBoolean chunkUnloaded = new AtomicBoolean(false);

    @Override
    public void onBlockEntityDetached(long chunkKey, int blockX, int blockY, int blockZ, String blockEntityId) {
        detachCount.incrementAndGet();
    }

    @Override
    public void onChunkUnloaded(long chunkKey) {
        chunkUnloaded.set(true);
    }

    /**
     * Returns the count of detach events received.
     *
     * @return detach event count
     */
    public int detachCount() {
        return detachCount.get();
    }

    /**
     * Returns whether the chunk unloaded event was received.
     *
     * @return {@code true} if unloaded
     */
    public boolean isChunkUnloaded() {
        return chunkUnloaded.get();
    }
}
