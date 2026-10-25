package dev.vessel.kernel.bootstrap;

/**
 * Functional contract for a single discrete step executed during a bootstrap phase.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface BootstrapStepAction {

    /**
     * Executes the bootstrap step operation.
     *
     * @throws Exception if step execution fails
     */
    void execute() throws Exception;
}
