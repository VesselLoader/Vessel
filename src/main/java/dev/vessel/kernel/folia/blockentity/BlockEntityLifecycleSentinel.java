package dev.vessel.kernel.folia.blockentity;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sentinel preventing ghost ticking and memory leaks during Folia chunk unloading.
 * <p>
 * Ensures that whenever a chunk is scheduled for unallocation or unload from RAM,
 * all registered BlockEntities (machines, pipes, energy conductors, quarries)
 * are cleanly detached from regional tick strands and external capability networks.
 * Attempts to tick detached BlockEntities are blocked and tracked.
 * </p>
 * <p>
 * Directly resolves ERR-36 and FOL-061..080.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BlockEntityLifecycleSentinel {

    private final ConcurrentMap<Long, ConcurrentMap<Long, BlockEntityRegistration>> chunkToEntitiesMap;
    private final AtomicLong totalDetachedEntitiesCount;
    private final AtomicLong ghostTicksPreventedCount;

    /**
     * Constructs a new BlockEntityLifecycleSentinel instance.
     */
    public BlockEntityLifecycleSentinel() {
        this.chunkToEntitiesMap = new ConcurrentHashMap<>();
        this.totalDetachedEntitiesCount = new AtomicLong(0L);
        this.ghostTicksPreventedCount = new AtomicLong(0L);
    }

    /**
     * Registers an active BlockEntity for lifecycle tracking within its owning chunk.
     *
     * @param chunkKey packed 64-bit coordinate of the chunk
     * @param blockX world X coordinate
     * @param blockY world Y coordinate
     * @param blockZ world Z coordinate
     * @param blockEntityId namespaced identifier of the BlockEntity
     * @param callback optional detachment listener (can be null)
     * @return {@code true} if successfully registered; {@code false} if already present
     */
    public boolean registerBlockEntity(
            long chunkKey,
            int blockX,
            int blockY,
            int blockZ,
            String blockEntityId,
            BlockEntityLifecycleCallback callback
    ) {
        Objects.requireNonNull(blockEntityId, "blockEntityId cannot be null");
        ConcurrentMap<Long, BlockEntityRegistration> entities =
                chunkToEntitiesMap.computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>());

        long posKey = packBlockPosition(blockX, blockY, blockZ);
        BlockEntityRegistration registration = new BlockEntityRegistration(
                chunkKey,
                blockX,
                blockY,
                blockZ,
                blockEntityId,
                BlockEntityTicketState.ACTIVE_TICKING,
                callback
        );

        return entities.putIfAbsent(posKey, registration) == null;
    }

    /**
     * Unregisters a BlockEntity when broken, mined, or destroyed naturally.
     *
     * @param chunkKey packed 64-bit coordinate of the chunk
     * @param blockX world X coordinate
     * @param blockY world Y coordinate
     * @param blockZ world Z coordinate
     * @return {@code true} if an existing registration was removed
     */
    public boolean unregisterBlockEntity(long chunkKey, int blockX, int blockY, int blockZ) {
        ConcurrentMap<Long, BlockEntityRegistration> entities = chunkToEntitiesMap.get(chunkKey);
        if (entities == null) {
            return false;
        }

        long posKey = packBlockPosition(blockX, blockY, blockZ);
        BlockEntityRegistration removed = entities.remove(posKey);
        if (entities.isEmpty()) {
            chunkToEntitiesMap.remove(chunkKey, entities);
        }
        return removed != null;
    }

    /**
     * Unloads an entire chunk from memory, cleanly detaching all BlockEntities and preventing ghost ticking.
     *
     * @param chunkKey packed 64-bit coordinate of the unloaded chunk
     * @return count of BlockEntities safely detached
     */
    public int onChunkUnload(long chunkKey) {
        ConcurrentMap<Long, BlockEntityRegistration> entities = chunkToEntitiesMap.remove(chunkKey);
        if (entities == null || entities.isEmpty()) {
            return 0;
        }

        int detachedCount = 0;
        for (Map.Entry<Long, BlockEntityRegistration> entry : entities.entrySet()) {
            BlockEntityRegistration reg = entry.getValue();
            BlockEntityRegistration detaching = reg.withState(BlockEntityTicketState.DETACHING);

            if (detaching.callback() != null) {
                try {
                    detaching.callback().onBlockEntityDetached(
                            reg.chunkKey(), reg.blockX(), reg.blockY(), reg.blockZ(), reg.blockEntityId()
                    );
                    detaching.callback().onChunkUnloaded(chunkKey);
                } catch (Throwable ignored) {
                    // Prevent faulty mod callback from aborting the detachment loop
                }
            }

            detachedCount++;
            totalDetachedEntitiesCount.incrementAndGet();
        }

        return detachedCount;
    }

    /**
     * Validates if a BlockEntity is legally allowed to execute its tick.
     * Prevents ghost ticking if the chunk has been unloaded.
     *
     * @param chunkKey packed 64-bit coordinate of the chunk
     * @param blockX world X coordinate
     * @param blockY world Y coordinate
     * @param blockZ world Z coordinate
     * @return {@code true} if active and permitted to tick; {@code false} if ghost ticking prevented
     */
    public boolean isTickingAllowed(long chunkKey, int blockX, int blockY, int blockZ) {
        ConcurrentMap<Long, BlockEntityRegistration> entities = chunkToEntitiesMap.get(chunkKey);
        if (entities == null) {
            ghostTicksPreventedCount.incrementAndGet();
            return false;
        }

        long posKey = packBlockPosition(blockX, blockY, blockZ);
        BlockEntityRegistration reg = entities.get(posKey);
        if (reg == null || reg.state() != BlockEntityTicketState.ACTIVE_TICKING) {
            ghostTicksPreventedCount.incrementAndGet();
            return false;
        }

        return true;
    }

    /**
     * Checks if a chunk is actively registered with tracked BlockEntities.
     *
     * @param chunkKey packed 64-bit coordinate of the chunk
     * @return {@code true} if active
     */
    public boolean isChunkActive(long chunkKey) {
        ConcurrentMap<Long, BlockEntityRegistration> entities = chunkToEntitiesMap.get(chunkKey);
        return entities != null && !entities.isEmpty();
    }

    /**
     * Returns the count of registered BlockEntities in a specific chunk.
     *
     * @param chunkKey packed 64-bit coordinate of the chunk
     * @return entity count in chunk
     */
    public int registeredCount(long chunkKey) {
        ConcurrentMap<Long, BlockEntityRegistration> entities = chunkToEntitiesMap.get(chunkKey);
        return entities != null ? entities.size() : 0;
    }

    /**
     * Returns the total count of registered BlockEntities across all chunks.
     *
     * @return total entity count
     */
    public int totalRegisteredCount() {
        int total = 0;
        for (ConcurrentMap<Long, BlockEntityRegistration> map : chunkToEntitiesMap.values()) {
            total += map.size();
        }
        return total;
    }

    /**
     * Returns the lifetime count of BlockEntities cleanly detached on chunk unloads.
     *
     * @return detached entities count
     */
    public long totalDetachedEntitiesCount() {
        return totalDetachedEntitiesCount.get();
    }

    /**
     * Returns the lifetime count of illegal ghost tick attempts blocked.
     *
     * @return blocked ghost ticks count
     */
    public long ghostTicksPreventedCount() {
        return ghostTicksPreventedCount.get();
    }

    /**
     * Clears all registrations and resets state.
     */
    public void clear() {
        chunkToEntitiesMap.clear();
    }

    /**
     * Packs 3D block coordinates into a compact 64-bit primitive long.
     */
    public static long packBlockPosition(int x, int y, int z) {
        return (((long) (x & 0x3FFFFFF)) << 38)
                | (((long) (z & 0x3FFFFFF)) << 12)
                | ((long) (y & 0xFFF));
    }
}
