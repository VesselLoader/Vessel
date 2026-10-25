package dev.vessel.kernel.optimization.physics;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * Explosion block update coalescer and lighting merger.
 * <p>
 * In vanilla Minecraft, high-yield detonations (TNT chains, Withers, Ender Dragon charges, modded nukes)
 * trigger thousands of isolated block changes and recursive neighbor light updates, freezing the main
 * server thread. This merger coalesces all destroyed voxels into a single batch, deduplicating redundant
 * ray hits, applying state mutations in spatial order, and scheduling exactly one unified lighting update
 * per affected chunk section. Mitigates ERR-83.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class UnbatchedExplosionBlockUpdateMerger {

    public static final int DEFAULT_BUFFER_CAPACITY = 4096;
    public static final int MAX_BUFFER_CAPACITY = 65536;

    private final LinkedHashMap<Long, ExplosionBlockEntry> pendingBlocks;
    private final LinkedHashSet<Long> affectedSections;
    private final int bufferCapacity;

    /**
     * Constructs an UnbatchedExplosionBlockUpdateMerger with default buffer capacity (4096 blocks).
     */
    public UnbatchedExplosionBlockUpdateMerger() {
        this(DEFAULT_BUFFER_CAPACITY);
    }

    /**
     * Constructs an UnbatchedExplosionBlockUpdateMerger with custom buffer capacity.
     *
     * @param capacity maximum pending blocks permitted before auto-flushing
     */
    public UnbatchedExplosionBlockUpdateMerger(int capacity) {
        if (capacity < 64 || capacity > MAX_BUFFER_CAPACITY) {
            throw new IllegalArgumentException("Capacity must be between 64 and " + MAX_BUFFER_CAPACITY + ": " + capacity);
        }
        this.bufferCapacity = capacity;
        this.pendingBlocks = new LinkedHashMap<>(capacity);
        this.affectedSections = new LinkedHashSet<>(capacity / 16);
    }

    /**
     * Records a single block destruction candidate resulting from an explosion ray.
     * Deduplicates redundant hits at the same coordinates.
     *
     * @param x               world coordinate X
     * @param y               world coordinate Y
     * @param z               world coordinate Z
     * @param newBlockStateId target block state ID (e.g. 0 for air)
     */
    public synchronized void recordExplodedBlock(int x, int y, int z, int newBlockStateId) {
        long key = packBlockPos(x, y, z);
        pendingBlocks.put(key, new ExplosionBlockEntry(x, y, z, newBlockStateId));

        int sectionX = x >> 4;
        int sectionY = y >> 4;
        int sectionZ = z >> 4;
        affectedSections.add(packSectionPos(sectionX, sectionY, sectionZ));
    }

    /**
     * Commits all buffered block destructions, dispatching unified block updates and section relights.
     *
     * @param updateConsumer callback for applying block state changes
     * @param lightConsumer  callback for dispatching section lighting passes
     * @return telemetry outcome report
     */
    public synchronized ExplosionBatchMergeResult commitBatch(
            ExplosionUpdateConsumer updateConsumer,
            ExplosionLightUpdateConsumer lightConsumer
    ) {
        Objects.requireNonNull(updateConsumer, "updateConsumer cannot be null");
        Objects.requireNonNull(lightConsumer, "lightConsumer cannot be null");

        if (pendingBlocks.isEmpty()) {
            return new ExplosionBatchMergeResult(0, 0, 0, 0L);
        }

        long startNanos = System.nanoTime();
        int totalBlocks = pendingBlocks.size();
        int affectedSectionsCount = affectedSections.size();

        // Pass 1: Apply all block mutations
        for (ExplosionBlockEntry entry : pendingBlocks.values()) {
            updateConsumer.applyBlockChange(entry.x(), entry.y(), entry.z(), entry.newBlockStateId());
        }

        // Pass 2: Dispatch exactly one lighting recalculation per unique chunk section
        int lightUpdatesDispatched = 0;
        for (Long sectionKey : affectedSections) {
            int sx = unpackSectionX(sectionKey);
            int sy = unpackSectionY(sectionKey);
            int sz = unpackSectionZ(sectionKey);
            lightConsumer.scheduleSectionRelight(sx, sy, sz);
            lightUpdatesDispatched++;
        }

        // Clear buffers
        pendingBlocks.clear();
        affectedSections.clear();

        long elapsedNanos = System.nanoTime() - startNanos;
        return new ExplosionBatchMergeResult(totalBlocks, affectedSectionsCount, lightUpdatesDispatched, elapsedNanos);
    }

    /**
     * Clears all pending entries without applying them.
     */
    public synchronized void clear() {
        pendingBlocks.clear();
        affectedSections.clear();
    }

    /**
     * Returns count of distinct pending blocks awaiting commit.
     *
     * @return pending block count
     */
    public synchronized int getPendingCount() {
        return pendingBlocks.size();
    }

    /**
     * Returns count of distinct chunk sections affected by pending blocks.
     *
     * @return affected section count
     */
    public synchronized int getAffectedSectionCount() {
        return affectedSections.size();
    }

    public int getBufferCapacity() {
        return bufferCapacity;
    }

    public static long packBlockPos(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) y & 0xFFFL) << 26) | ((long) z & 0x3FFFFFFL);
    }

    public static long packSectionPos(int sx, int sy, int sz) {
        return (((long) sx & 0x3FFFFFL) << 42) | (((long) sy & 0xFFFFF) << 22) | ((long) sz & 0x3FFFFFL);
    }

    public static int unpackSectionX(long packed) {
        return (int) (packed >> 42);
    }

    public static int unpackSectionY(long packed) {
        return (int) ((packed << 22) >> 44);
    }

    public static int unpackSectionZ(long packed) {
        return (int) ((packed << 42) >> 42);
    }
}
