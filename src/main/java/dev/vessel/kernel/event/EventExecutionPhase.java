package dev.vessel.kernel.event;

/**
 * Universal execution priority phase for event listener dispatch.
 * <p>
 * Bridges the differing event priority models across loaders:
 * <ul>
 *   <li>Bukkit: LOWEST (executed first) to HIGHEST (executed last) and MONITOR.</li>
 *   <li>Forge / NeoForge: HIGHEST (executed first) to LOWEST (executed last).</li>
 *   <li>Fabric / Quilt: Phase ordering DAG (FIRST, DEFAULT, LAST).</li>
 * </ul>
 * Standardizes execution order into 6 deterministic sequential tiers.
 *
 * @version 1.0.0
 */
public enum EventExecutionPhase {

    /**
     * Executed first before any state modifications occur.
     */
    EARLIEST(0),

    /**
     * Executed before the normal game logic phase.
     */
    EARLY(1),

    /**
     * Default execution phase for standard mod and plugin listeners.
     */
    NORMAL(2),

    /**
     * Executed after standard logic to allow late adjustments.
     */
    LATE(3),

    /**
     * Executed last among mutating listeners, having final say on event outcome.
     */
    LATEST(4),

    /**
     * Read-only inspection phase for logging, metrics, and diagnostics.
     * Must not mutate the event or cancel it.
     */
    MONITOR(5);

    private final int order;

    EventExecutionPhase(int order) {
        this.order = order;
    }

    /**
     * Returns the numeric execution order (0 executes first, 5 executes last).
     *
     * @return numeric order
     */
    public int getOrder() {
        return order;
    }

    /**
     * Maps a Bukkit EventPriority name to the universal phase.
     *
     * @param bukkitPriority Bukkit priority name (e.g., "LOWEST", "NORMAL", "MONITOR")
     * @return corresponding universal phase
     */
    public static EventExecutionPhase fromBukkit(String bukkitPriority) {
        if (bukkitPriority == null) {
            return NORMAL;
        }
        return switch (bukkitPriority.toUpperCase().trim()) {
            case "LOWEST" -> EARLIEST;
            case "LOW" -> EARLY;
            case "NORMAL" -> NORMAL;
            case "HIGH" -> LATE;
            case "HIGHEST" -> LATEST;
            case "MONITOR" -> MONITOR;
            default -> NORMAL;
        };
    }

    /**
     * Maps a Forge/NeoForge EventPriority name to the universal phase.
     *
     * @param forgePriority Forge priority name (e.g., "HIGHEST", "NORMAL", "LOWEST")
     * @return corresponding universal phase
     */
    public static EventExecutionPhase fromForge(String forgePriority) {
        if (forgePriority == null) {
            return NORMAL;
        }
        return switch (forgePriority.toUpperCase().trim()) {
            case "HIGHEST" -> EARLIEST;
            case "HIGH" -> EARLY;
            case "NORMAL" -> NORMAL;
            case "LOW" -> LATE;
            case "LOWEST" -> LATEST;
            default -> NORMAL;
        };
    }
}
