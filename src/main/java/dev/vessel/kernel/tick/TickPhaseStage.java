package dev.vessel.kernel.tick;

/**
 * Discrete execution phases comprising a canonical 50ms (20.0 TPS) server game tick.
 * <p>
 * Ensures strict deterministic sequencing between network I/O, global region scheduling,
 * regional actor strand execution, and packet batch flushing.
 * </p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-6)
 */
public enum TickPhaseStage {

    /** Pre-tick initialization and timer synchronizations. */
    PRE_TICK,

    /** Polling incoming network packets across Java and Bedrock sockets. */
    POLL_NETWORK,

    /** Executing the global region strand tick (legacy Bukkit plugins and world time). */
    TICK_GLOBAL_STRAND,

    /** Concurrent tick execution across all active RegionalActorStrands (16x16 chunk actors). */
    TICK_REGIONAL_STRANDS,

    /** Cross-region entity transitions and world boundary reconciliations. */
    POST_ENTITY_TICK,

    /** Flushing batched outgoing packet frames over network transport. */
    FLUSH_NETWORK,

    /** Post-tick metrics recording, MSPT calculation, and time-budget reconciliation. */
    POST_TICK
}
