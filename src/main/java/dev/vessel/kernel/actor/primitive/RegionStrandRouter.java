package dev.vessel.kernel.actor.primitive;

import dev.vessel.kernel.actor.RegionCoordinateHash;
import dev.vessel.kernel.actor.RegionalActorStrand;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Ultra-fast, zero-allocation spatial router for dispatching block and chunk coordinates
 * to the owning {@link RegionalActorStrand}.
 * <p>
 * Employs direct bitshift coordinate arithmetic:
 * <pre>
 * regionX = blockX &gt;&gt; 8 = chunkX &gt;&gt; 4
 * regionZ = blockZ &gt;&gt; 8 = chunkZ &gt;&gt; 4
 * packedKey = (((long) regionX) &lt;&lt; 32) | (regionZ &amp; 0xFFFFFFFFL)
 * </pre>
 * Combined with {@link LongPrimitive2ObjectMap}, spatial routing executes with zero heap allocations.
 * </p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-5)
 */
public class RegionStrandRouter {

    private final LongPrimitive2ObjectMap<RegionalActorStrand> map = new LongPrimitive2ObjectMap<>(64);
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    /**
     * Registers a regional actor strand into the spatial routing table.
     *
     * @param strand the strand to register
     */
    public void register(RegionalActorStrand strand) {
        Objects.requireNonNull(strand, "strand cannot be null");
        long key = strand.getRegionHash().toPackedKey();
        rwl.writeLock().lock();
        try {
            map.put(key, strand);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    /**
     * Unregisters a regional actor strand from the spatial routing table.
     *
     * @param hash the region coordinate hash to unregister
     * @return the removed strand or null if absent
     */
    public RegionalActorStrand unregister(RegionCoordinateHash hash) {
        Objects.requireNonNull(hash, "hash cannot be null");
        long key = hash.toPackedKey();
        rwl.writeLock().lock();
        try {
            return map.remove(key);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    /**
     * Routes spatial block coordinates directly to the assigned RegionalActorStrand with ZERO heap allocations.
     *
     * @param blockX world block X coordinate
     * @param blockZ world block Z coordinate
     * @return RegionalActorStrand or null if unassigned
     */
    public RegionalActorStrand routeByBlock(int blockX, int blockZ) {
        int regX = blockX >> 8;
        int regZ = blockZ >> 8;
        long packedKey = (((long) regX) << 32) | (regZ & 0xFFFFFFFFL);
        return routeByPackedKey(packedKey);
    }

    /**
     * Routes chunk coordinates directly to the assigned RegionalActorStrand with ZERO heap allocations.
     *
     * @param chunkX world chunk X coordinate
     * @param chunkZ world chunk Z coordinate
     * @return RegionalActorStrand or null if unassigned
     */
    public RegionalActorStrand routeByChunk(int chunkX, int chunkZ) {
        int regX = chunkX >> 4;
        int regZ = chunkZ >> 4;
        long packedKey = (((long) regX) << 32) | (regZ & 0xFFFFFFFFL);
        return routeByPackedKey(packedKey);
    }

    /**
     * Fast-path lookup by pre-computed 64-bit packed region key.
     *
     * @param packedKey 64-bit packed region coordinate
     * @return RegionalActorStrand or null if absent
     */
    public RegionalActorStrand routeByPackedKey(long packedKey) {
        rwl.readLock().lock();
        try {
            return map.get(packedKey);
        } finally {
            rwl.readLock().unlock();
        }
    }

    /**
     * Returns total count of registered regional strands.
     *
     * @return registered strand count
     */
    public int registeredCount() {
        rwl.readLock().lock();
        try {
            return map.size();
        } finally {
            rwl.readLock().unlock();
        }
    }

    /**
     * Clears all registered routing table entries.
     */
    public void clear() {
        rwl.writeLock().lock();
        try {
            map.clear();
        } finally {
            rwl.writeLock().unlock();
        }
    }
}
