package dev.vessel.kernel.adapter.fabric;

/**
 * Exception thrown when a cyclic dependency is detected within a DAG event registration graph.
 *
 * <p>Quells: ERR-57.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DagCycleDetectedException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new DagCycleDetectedException with an explanatory message.
     *
     * @param message description of the cycle
     */
    public DagCycleDetectedException(String message) {
        super(message);
    }

    /**
     * Constructs a new DagCycleDetectedException with message and cause.
     *
     * @param message description of the cycle
     * @param cause underlying cause
     */
    public DagCycleDetectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
