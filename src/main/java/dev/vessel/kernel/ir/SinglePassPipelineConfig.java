package dev.vessel.kernel.ir;

/**
 * Immutable configuration settings for execution within the {@link SinglePassBytecodePipeline}.
 *
 * @param expandFrames whether to expand stack map frames during class reading
 * @param computeFrames whether ASM ClassWriter should automatically recompute stack map frames
 * @param computeMaxs whether ASM ClassWriter should recompute max stack and max locals
 * @param reconcileSignatures whether to validate and reconcile generic signatures against raw descriptors
 * @param deduplicateBridges whether to detect and eliminate duplicate covariant synthetic bridge methods
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record SinglePassPipelineConfig(
        boolean expandFrames,
        boolean computeFrames,
        boolean computeMaxs,
        boolean reconcileSignatures,
        boolean deduplicateBridges
) {
    /**
     * Default production pipeline configuration tuned for maximum throughput and compatibility.
     */
    public static final SinglePassPipelineConfig DEFAULT = new SinglePassPipelineConfig(
            true,
            false,
            true,
            true,
            true
    );

    /**
     * Full recalculation configuration when heavy bytecode alterations are performed.
     */
    public static final SinglePassPipelineConfig FULL_COMPUTE = new SinglePassPipelineConfig(
            true,
            true,
            true,
            true,
            true
    );
}
