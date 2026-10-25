package dev.vessel.kernel.optimization.entity;

import java.util.Arrays;

/**
 * High-performance, zero-allocation entity AI memory storage inspired by Canvas and Purpur.
 * Replaces bulky heap-allocated {@code Map<MemoryModuleType, Optional<ExpirableValue>>}
 * instances with a compact, contiguous primitive {@code long[]} array.
 *
 * <p>Each 64-bit slot encodes:
 * <pre>
 * [63..48]: 16-bit memory type identifier
 * [47..0]:  48-bit absolute expiry tick
 * </pre>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class EntityMemoryCompactTable {

    private static final int DEFAULT_CAPACITY = 8;
    private static final long EMPTY_SLOT = -1L;

    private long[] slots;
    private int size;

    public EntityMemoryCompactTable(int initialCapacity) {
        int cap = Math.max(initialCapacity, DEFAULT_CAPACITY);
        this.slots = new long[cap];
        Arrays.fill(this.slots, EMPTY_SLOT);
        this.size = 0;
    }

    public EntityMemoryCompactTable() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Stores or updates an entity memory entry with an expiration tick.
     *
     * @param memoryTypeId 16-bit identifier for the memory module
     * @param expiryTick absolute game tick when the memory expires
     */
    public void setMemory(int memoryTypeId, long expiryTick) {
        if (memoryTypeId < 0 || memoryTypeId > 0xFFFF) {
            throw new IllegalArgumentException("memoryTypeId must fit in 16 bits (0..65535): " + memoryTypeId);
        }

        long packed = (((long) memoryTypeId) << 48) | (expiryTick & 0x0000FFFFFFFFFFFFL);

        // Check if memory already exists
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != EMPTY_SLOT && getTypeId(slots[i]) == memoryTypeId) {
                slots[i] = packed;
                return;
            }
        }

        // Insert into first empty slot or expand
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == EMPTY_SLOT) {
                slots[i] = packed;
                size++;
                return;
            }
        }

        // Expand capacity
        int oldLength = slots.length;
        slots = Arrays.copyOf(slots, oldLength * 2);
        Arrays.fill(slots, oldLength, slots.length, EMPTY_SLOT);
        slots[oldLength] = packed;
        size++;
    }

    /**
     * Checks if a valid, non-expired memory exists for the specified type.
     *
     * @param memoryTypeId 16-bit identifier
     * @param currentTick current game tick
     * @return true if memory exists and expiryTick > currentTick
     */
    public boolean hasMemory(int memoryTypeId, long currentTick) {
        for (long slot : slots) {
            if (slot != EMPTY_SLOT && getTypeId(slot) == memoryTypeId) {
                long expiry = getExpiryTick(slot);
                return expiry > currentTick;
            }
        }
        return false;
    }

    /**
     * Clears a specific memory type from the table.
     *
     * @param memoryTypeId identifier to remove
     * @return true if an entry was cleared
     */
    public boolean clearMemory(int memoryTypeId) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != EMPTY_SLOT && getTypeId(slots[i]) == memoryTypeId) {
                slots[i] = EMPTY_SLOT;
                size--;
                return true;
            }
        }
        return false;
    }

    /**
     * Purges all expired memories relative to the current tick.
     *
     * @param currentTick current game tick
     * @return number of purged expired memories
     */
    public int purgeExpired(long currentTick) {
        int purged = 0;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != EMPTY_SLOT) {
                long expiry = getExpiryTick(slots[i]);
                if (expiry <= currentTick) {
                    slots[i] = EMPTY_SLOT;
                    size--;
                    purged++;
                }
            }
        }
        return purged;
    }

    public int size() {
        return size;
    }

    private static int getTypeId(long packed) {
        return (int) ((packed >>> 48) & 0xFFFF);
    }

    private static long getExpiryTick(long packed) {
        return packed & 0x0000FFFFFFFFFFFFL;
    }
}
