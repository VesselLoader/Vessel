package dev.vessel.kernel.optimization.physics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lithium-inspired bitmask tracker for entity equipment and armor changes.
 * <p>
 * In vanilla Minecraft, living entities inspect and compare all 6 equipment slots
 * (main hand, offhand, boots, leggings, chestplate, helmet) on every tick,
 * performing redundant ItemStack comparisons and NBT diffs.
 * This tracker maintains a 6-bit dirty mask. When no equipment has changed,
 * the entire equipment tick and network synchronization loop is bypassed in 1 CPU cycle.
 *
 * @version 1.0.0
 */
public final class EntityEquipmentChangeMask {

    public static final int SLOT_MAIN_HAND = 0;
    public static final int SLOT_OFF_HAND = 1;
    public static final int SLOT_FEET = 2;
    public static final int SLOT_LEGS = 3;
    public static final int SLOT_CHEST = 4;
    public static final int SLOT_HEAD = 5;

    private static final int ALL_SLOTS_MASK = 0x3F; // 6 bits: 0b111111

    private final AtomicInteger dirtyMask;
    private final AtomicLong totalTickChecks;
    private final AtomicLong skippedTicks;

    /**
     * Constructs a new equipment change tracker with all slots clean.
     */
    public EntityEquipmentChangeMask() {
        this.dirtyMask = new AtomicInteger(0);
        this.totalTickChecks = new AtomicLong(0L);
        this.skippedTicks = new AtomicLong(0L);
    }

    /**
     * Marks a specific equipment slot as changed/dirty.
     *
     * @param slotIndex equipment slot index (0 to 5)
     */
    public void markDirty(int slotIndex) {
        if (slotIndex < 0 || slotIndex > 5) {
            throw new IndexOutOfBoundsException("Slot index must be between 0 and 5: " + slotIndex);
        }
        dirtyMask.updateAndGet(mask -> mask | (1 << slotIndex));
    }

    /**
     * Marks all equipment slots as dirty (e.g., initial entity spawn or full resync).
     */
    public void markAllDirty() {
        dirtyMask.set(ALL_SLOTS_MASK);
    }

    /**
     * Checks if any equipment slot is dirty without modifying the state.
     *
     * @return true if at least one slot changed
     */
    public boolean hasChanges() {
        totalTickChecks.incrementAndGet();
        boolean hasDirty = dirtyMask.get() != 0;
        if (!hasDirty) {
            skippedTicks.incrementAndGet();
        }
        return hasDirty;
    }

    /**
     * Checks whether a specific slot is dirty.
     *
     * @param slotIndex equipment slot index (0 to 5)
     * @return true if the slot is dirty
     */
    public boolean isSlotDirty(int slotIndex) {
        if (slotIndex < 0 || slotIndex > 5) {
            throw new IndexOutOfBoundsException("Slot index must be between 0 and 5: " + slotIndex);
        }
        return (dirtyMask.get() & (1 << slotIndex)) != 0;
    }

    /**
     * Consumes the current dirty bitmask and resets it to clean (0).
     *
     * @return the bitmask of slots that were modified before the reset
     */
    public int consumeAndClear() {
        return dirtyMask.getAndSet(0);
    }

    /**
     * Returns total tick queries evaluated.
     *
     * @return count
     */
    public long getTotalTickChecks() {
        return totalTickChecks.get();
    }

    /**
     * Returns total ticks where equipment diffing was completely skipped due to a clean mask.
     *
     * @return count
     */
    public long getSkippedTicks() {
        return skippedTicks.get();
    }

    /**
     * Returns the ratio of skipped ticks (0.0 to 1.0).
     *
     * @return skip ratio
     */
    public double getSkipRatio() {
        long total = totalTickChecks.get();
        if (total == 0L) {
            return 1.0;
        }
        return (double) skippedTicks.get() / (double) total;
    }
}
