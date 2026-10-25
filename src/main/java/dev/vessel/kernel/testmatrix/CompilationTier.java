package dev.vessel.kernel.testmatrix;

/**
 * Compilation tiers denoting JIT compilation urgency and optimization priority.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum CompilationTier {

    /** Tier 4 / C2 hot-path compilation with aggressive inlining, vectorization, and escape analysis. */
    C2_HOT_PATH(1, 15_000),

    /** Tier 3 / C1 warm compilation with method profiling. */
    C1_WARM_PATH(2, 2_000),

    /** Cold bootstrap or shutdown logic with standard tiered interpreter execution. */
    TIERED_COLD_PATH(3, 500);

    private final int priorityOrder;
    private final int defaultWarmupInvocations;

    CompilationTier(int priorityOrder, int defaultWarmupInvocations) {
        this.priorityOrder = priorityOrder;
        this.defaultWarmupInvocations = defaultWarmupInvocations;
    }

    public int priorityOrder() {
        return priorityOrder;
    }

    public int defaultWarmupInvocations() {
        return defaultWarmupInvocations;
    }
}
