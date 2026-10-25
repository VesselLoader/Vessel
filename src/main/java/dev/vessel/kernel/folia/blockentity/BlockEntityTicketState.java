package dev.vessel.kernel.folia.blockentity;

/**
 * Lifecycle states of a BlockEntity ticket managed by the Folia Sentinel.
 * <p>
 * Directly resolves ERR-36 and FOL-061..080.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum BlockEntityTicketState {

    /**
     * Active and ticking normally inside an active Folia region strand.
     */
    ACTIVE_TICKING,

    /**
     * In the process of detaching from energy grids, network pipes, and tick lists.
     */
    DETACHING,

    /**
     * Safely detached from all cross-region networks and tick loops.
     */
    DETACHED,

    /**
     * Owning chunk has been unloaded from RAM; all references invalidated.
     */
    UNLOADED,

    /**
     * Permanently destroyed or garbage collected.
     */
    DISPOSED
}
