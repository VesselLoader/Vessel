package dev.vessel.kernel.testmatrix;

/**
 * Functional callback interface invoked when a branchless table switch route matches an opcode index.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface BranchlessRouteAction {

    /**
     * Executes the routed branchless operation.
     *
     * @param opcode dispatch opcode index
     * @param payload packed argument payload or entity identifier
     */
    void execute(int opcode, long payload);
}
