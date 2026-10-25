package dev.vessel.kernel.folia.blockentity;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GC-Safe Capability Bridge eliminating cross-chunk memory leaks in Folia networks.
 * <p>
 * Eliminates massive multi-gigabyte memory leaks (ERR-37 and FOL-081..100) caused by
 * long-distance cable systems, wireless grids, and pipes caching hard references
 * to BlockEntities across chunk boundaries. All capabilities are bridged via
 * ephemeral {@link WeakReference} handles and purged automatically on chunk unloads.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class WeakReferenceCapabilityBridge {

    private final ConcurrentMap<Long, ConcurrentMap<String, WeakCapabilityEntry>> storage;
    private final AtomicLong stalePurgedCount;

    /**
     * Constructs a new WeakReferenceCapabilityBridge.
     */
    public WeakReferenceCapabilityBridge() {
        this.storage = new ConcurrentHashMap<>();
        this.stalePurgedCount = new AtomicLong(0L);
    }

    /**
     * Registers a machine capability using an ephemeral weak reference.
     *
     * @param <T> capability instance type
     * @param chunkKey packed 64-bit coordinate of the host chunk
     * @param blockX world X coordinate
     * @param blockY world Y coordinate
     * @param blockZ world Z coordinate
     * @param capabilityId namespaced identifier for the capability
     * @param capabilityInstance non-null capability implementation
     * @throws NullPointerException if arguments are null
     */
    public <T> void registerCapability(
            long chunkKey,
            int blockX,
            int blockY,
            int blockZ,
            String capabilityId,
            T capabilityInstance
    ) {
        Objects.requireNonNull(capabilityId, "capabilityId cannot be null");
        Objects.requireNonNull(capabilityInstance, "capabilityInstance cannot be null");

        ConcurrentMap<String, WeakCapabilityEntry> chunkMap =
                storage.computeIfAbsent(chunkKey, k -> new ConcurrentHashMap<>());

        String slotKey = buildSlotKey(blockX, blockY, blockZ, capabilityId);
        WeakCapabilityEntry entry = new WeakCapabilityEntry(
                chunkKey,
                blockX,
                blockY,
                blockZ,
                capabilityId,
                new WeakReference<>(capabilityInstance)
        );

        chunkMap.put(slotKey, entry);
    }

    /**
     * Resolves a capability from a target block position if still reachable and valid.
     * Automatically purges the entry if the referent has been garbage collected.
     *
     * @param <T> requested capability type
     * @param chunkKey packed 64-bit coordinate of the target chunk
     * @param blockX world X coordinate
     * @param blockY world Y coordinate
     * @param blockZ world Z coordinate
     * @param capabilityId namespaced identifier for the capability
     * @param expectedType class type required by caller
     * @return Optional containing the capability if reachable; empty if unloaded or GC-collected
     */
    public <T> Optional<T> getCapability(
            long chunkKey,
            int blockX,
            int blockY,
            int blockZ,
            String capabilityId,
            Class<T> expectedType
    ) {
        Objects.requireNonNull(capabilityId, "capabilityId cannot be null");
        Objects.requireNonNull(expectedType, "expectedType cannot be null");

        ConcurrentMap<String, WeakCapabilityEntry> chunkMap = storage.get(chunkKey);
        if (chunkMap == null) {
            return Optional.empty();
        }

        String slotKey = buildSlotKey(blockX, blockY, blockZ, capabilityId);
        WeakCapabilityEntry entry = chunkMap.get(slotKey);
        if (entry == null) {
            return Optional.empty();
        }

        T typed = entry.get(expectedType);
        if (typed == null) {
            // Referent collected or type mismatch - purge stale entry
            chunkMap.remove(slotKey, entry);
            stalePurgedCount.incrementAndGet();
            return Optional.empty();
        }

        return Optional.of(typed);
    }

    /**
     * Checks if an active, reachable capability entry exists for the given slot.
     *
     * @param chunkKey packed 64-bit coordinate of the chunk
     * @param blockX world X coordinate
     * @param blockY world Y coordinate
     * @param blockZ world Z coordinate
     * @param capabilityId namespaced identifier for the capability
     * @return {@code true} if present and still reachable in RAM
     */
    public boolean hasCapability(long chunkKey, int blockX, int blockY, int blockZ, String capabilityId) {
        ConcurrentMap<String, WeakCapabilityEntry> chunkMap = storage.get(chunkKey);
        if (chunkMap == null) {
            return false;
        }

        String slotKey = buildSlotKey(blockX, blockY, blockZ, capabilityId);
        WeakCapabilityEntry entry = chunkMap.get(slotKey);
        return entry != null && entry.isReachable();
    }

    /**
     * Notifies the bridge that a chunk has unloaded, instantly purging all weak capability entries.
     *
     * @param chunkKey packed 64-bit coordinate of the unloaded chunk
     * @return count of capabilities removed
     */
    public int onChunkUnload(long chunkKey) {
        ConcurrentMap<String, WeakCapabilityEntry> removed = storage.remove(chunkKey);
        return removed != null ? removed.size() : 0;
    }

    /**
     * Actively sweeps and purges dead or collected weak references across all tracked chunks.
     *
     * @return count of stale entries purged during this pass
     */
    public int cleanStaleReferences() {
        int purged = 0;

        for (Iterator<Map.Entry<Long, ConcurrentMap<String, WeakCapabilityEntry>>> chunkIt =
             storage.entrySet().iterator(); chunkIt.hasNext(); ) {
            Map.Entry<Long, ConcurrentMap<String, WeakCapabilityEntry>> chunkEntry = chunkIt.next();
            ConcurrentMap<String, WeakCapabilityEntry> map = chunkEntry.getValue();

            for (Iterator<Map.Entry<String, WeakCapabilityEntry>> it = map.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, WeakCapabilityEntry> entry = it.next();
                if (!entry.getValue().isReachable()) {
                    it.remove();
                    purged++;
                    stalePurgedCount.incrementAndGet();
                }
            }

            if (map.isEmpty()) {
                chunkIt.remove();
            }
        }

        return purged;
    }

    /**
     * Returns the total active capability entries currently held.
     *
     * @return total entries count
     */
    public int totalEntryCount() {
        int count = 0;
        for (ConcurrentMap<String, WeakCapabilityEntry> map : storage.values()) {
            count += map.size();
        }
        return count;
    }

    /**
     * Returns the lifetime count of stale references purged by GC sweeps or get checks.
     *
     * @return purged stale references count
     */
    public long stalePurgedCount() {
        return stalePurgedCount.get();
    }

    /**
     * Clears all registrations.
     */
    public void clear() {
        storage.clear();
    }

    private static String buildSlotKey(int x, int y, int z, String capId) {
        return x + "/" + y + "/" + z + "/" + capId;
    }
}
