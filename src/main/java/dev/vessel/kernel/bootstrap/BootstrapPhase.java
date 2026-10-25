package dev.vessel.kernel.bootstrap;

import java.util.Objects;

/**
 * Enumeration representing the discrete lifecycle phases of the VESSEL microkernel bootstrap sequence.
 * Execution flows strictly sequentially from {@link #PREFLIGHT} to {@link #COMPLETED}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum BootstrapPhase {

    /** Sanity validation of JVM version, system architecture, and execution flags. */
    PREFLIGHT(0, "PREFLIGHT"),

    /** Bytecode scanning and symbol extraction across ecosystem modules. */
    SCANNER(1, "SCANNER"),

    /** Virtual file system layered mount initialization and asset binding. */
    VFS_MOUNT(2, "VFS_MOUNT"),

    /** Native binary deduplication and unsatisfied link protection initialization. */
    NATIVE_GUARD(3, "NATIVE_GUARD"),

    /** Topological module sorting and sovereign registry binding. */
    REGISTRY(4, "REGISTRY"),

    /** Bootstrap sequence finalized successfully; game loop ready to launch. */
    COMPLETED(5, "COMPLETED"),

    /** Bootstrap sequence aborted due to an unrecoverable failure. */
    FAILED(-1, "FAILED");

    private final int orderIndex;
    private final String identifier;

    BootstrapPhase(int orderIndex, String identifier) {
        this.orderIndex = orderIndex;
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
    }

    /**
     * Returns the execution sequence index of this phase.
     *
     * @return sequence order index
     */
    public int orderIndex() {
        return orderIndex;
    }

    /**
     * Returns the human-readable identifier of this phase.
     *
     * @return phase identifier string
     */
    public String identifier() {
        return identifier;
    }

    /**
     * Indicates whether this phase represents a terminal state.
     *
     * @return true if COMPLETED or FAILED
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
