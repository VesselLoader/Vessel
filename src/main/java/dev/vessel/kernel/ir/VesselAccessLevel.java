package dev.vessel.kernel.ir;

/**
 * Enumeration representing access modifier visibility levels in VESSEL IR.
 *
 * <p>Used in the {@code VesselAccessWideningTable} to unify Fabric Access Wideners and Forge/NeoForge
 * Access Transformers into a single declarative visibility representation.</p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-3)
 */
public enum VesselAccessLevel {

    PRIVATE(0),
    PACKAGE_PRIVATE(1),
    PROTECTED(2),
    PUBLIC(3);

    private final int rank;

    VesselAccessLevel(int rank) {
        this.rank = rank;
    }

    /**
     * Checks if this access level is more permissive or equal to another level.
     *
     * @param other the access level to compare against
     * @return true if this level is at least as wide as other
     */
    public boolean isAtLeast(VesselAccessLevel other) {
        return this.rank >= other.rank;
    }

    /**
     * Computes the widest access level between two requested levels.
     *
     * @param a first level
     * @param b second level
     * @return the wider access level
     */
    public static VesselAccessLevel widest(VesselAccessLevel a, VesselAccessLevel b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.rank >= b.rank ? a : b;
    }
}
