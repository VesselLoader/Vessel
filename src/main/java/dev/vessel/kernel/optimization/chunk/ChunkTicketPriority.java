package dev.vessel.kernel.optimization.chunk;

/**
 * Priority tier definitions for chunk tickets governing load ordering and automated eviction.
 * Lower tier index indicates higher priority.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum ChunkTicketPriority {
    PLAYER(1, 31),
    ENTITY(2, 2),
    PATHFINDING(3, 1),
    WORLD_GEN(4, 0),
    BACKGROUND(5, 0);

    private final int tierIndex;
    private final int defaultRadius;

    ChunkTicketPriority(int tierIndex, int defaultRadius) {
        this.tierIndex = tierIndex;
        this.defaultRadius = defaultRadius;
    }

    public int tierIndex() {
        return tierIndex;
    }

    public int defaultRadius() {
        return defaultRadius;
    }

    /**
     * Checks if this priority is higher than another priority.
     *
     * @param other priority to compare
     * @return true if this priority takes precedence
     */
    public boolean isHigherPriorityThan(ChunkTicketPriority other) {
        return this.tierIndex < other.tierIndex;
    }
}
