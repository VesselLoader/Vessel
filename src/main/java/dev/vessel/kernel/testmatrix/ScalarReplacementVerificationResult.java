package dev.vessel.kernel.testmatrix;

/**
 * Immutable report validating whether scalar replacement succeeded in achieving 0-byte heap allocations.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param allocatedBytesBefore thread allocated bytes before execution
 * @param allocatedBytesAfter thread allocated bytes after execution
 * @param netAllocatedBytes net difference in heap allocation (0 if fully scalar replaced)
 * @param zeroAllocationAchieved true if netAllocatedBytes == 0
 * @param operationsExecuted total scalar operations performed
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ScalarReplacementVerificationResult(
        long allocatedBytesBefore,
        long allocatedBytesAfter,
        long netAllocatedBytes,
        boolean zeroAllocationAchieved,
        long operationsExecuted
) {
}
