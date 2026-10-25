package dev.vessel.kernel.optimization.spatial;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Sodium-inspired primitive bitmask tracker for vertical chunk sections.
 * <p>
 * In Minecraft 1.18+, world height spans 24 sections (-64 to 320), and custom dimensions
 * support up to 64 vertical sections per chunk column.
 * Traditional loaders maintain object sets or boolean arrays to track non-empty or visible sections.
 * This class packs 64 section status flags into a single primitive {@code long},
 * leveraging native CPU bitwise instructions (POPCNT, TZCNT, LZCNT) for zero-allocation
 * spatial queries.
 *
 * @version 1.0.0
 */
public final class ChunkSectionSpatialBitSet {

    public static final int MAX_SECTIONS = 64;

    private final AtomicLong mask;

    /**
     * Constructs an empty spatial bit set with all sections inactive.
     */
    public ChunkSectionSpatialBitSet() {
        this(0L);
    }

    /**
     * Constructs a spatial bit set initialized with the given raw bitmask.
     *
     * @param initialMask raw 64-bit mask
     */
    public ChunkSectionSpatialBitSet(long initialMask) {
        this.mask = new AtomicLong(initialMask);
    }

    /**
     * Sets the specified section index (0 to 63) as active.
     *
     * @param sectionIndex vertical section index
     */
    public void setSectionActive(int sectionIndex) {
        validateSectionIndex(sectionIndex);
        mask.updateAndGet(m -> m | (1L << sectionIndex));
    }

    /**
     * Clears the specified section index (0 to 63).
     *
     * @param sectionIndex vertical section index
     */
    public void clearSection(int sectionIndex) {
        validateSectionIndex(sectionIndex);
        mask.updateAndGet(m -> m & ~(1L << sectionIndex));
    }

    /**
     * Checks if the specified section index is active.
     *
     * @param sectionIndex vertical section index
     * @return true if active
     */
    public boolean hasSection(int sectionIndex) {
        validateSectionIndex(sectionIndex);
        return (mask.get() & (1L << sectionIndex)) != 0L;
    }

    /**
     * Returns the total count of active sections using hardware POPCNT.
     *
     * @return active section count (0 to 64)
     */
    public int countActiveSections() {
        return Long.bitCount(mask.get());
    }

    /**
     * Returns the index of the lowest active section, or -1 if no sections are active.
     *
     * @return lowest section index or -1
     */
    public int getLowestActiveSection() {
        long m = mask.get();
        if (m == 0L) {
            return -1;
        }
        return Long.numberOfTrailingZeros(m);
    }

    /**
     * Returns the index of the highest active section, or -1 if no sections are active.
     *
     * @return highest section index or -1
     */
    public int getHighestActiveSection() {
        long m = mask.get();
        if (m == 0L) {
            return -1;
        }
        return 63 - Long.numberOfLeadingZeros(m);
    }

    /**
     * Checks if this bit set shares any active sections with another bit set.
     *
     * @param other another spatial bit set
     * @return true if they intersect
     */
    public boolean intersects(ChunkSectionSpatialBitSet other) {
        if (other == null) {
            return false;
        }
        return (this.mask.get() & other.mask.get()) != 0L;
    }

    /**
     * Returns the raw 64-bit mask.
     *
     * @return bitmask
     */
    public long getRawMask() {
        return mask.get();
    }

    /**
     * Clears all sections.
     */
    public void clearAll() {
        mask.set(0L);
    }

    private static void validateSectionIndex(int index) {
        if (index < 0 || index >= MAX_SECTIONS) {
            throw new IndexOutOfBoundsException("Section index out of bounds: " + index + ". Expected 0 to 63.");
        }
    }
}
