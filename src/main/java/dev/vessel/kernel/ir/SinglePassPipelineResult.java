package dev.vessel.kernel.ir;

import java.util.Objects;

/**
 * Immutable report representing the outcome of a {@link SinglePassBytecodePipeline} execution.
 *
 * @param bytecode the resulting class bytecode (modified or original byte array)
 * @param modified true if any transformation was performed on the bytecode
 * @param transformationsApplied count of modifications successfully applied
 * @param durationNanos execution time in nanoseconds
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record SinglePassPipelineResult(
        byte[] bytecode,
        boolean modified,
        int transformationsApplied,
        long durationNanos
) {
    public SinglePassPipelineResult {
        Objects.requireNonNull(bytecode, "bytecode cannot be null");
    }
}
