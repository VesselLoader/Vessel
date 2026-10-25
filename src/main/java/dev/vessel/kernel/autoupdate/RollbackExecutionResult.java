package dev.vessel.kernel.autoupdate;

import java.util.Objects;

/**
 * Immutable record documenting the outcome of an automated rollback execution.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param snapshotId               id of the restored snapshot
 * @param restoredSlot             the binary slot restored to active status
 * @param rolledBackSuccessfully   {@code true} if restoration succeeded
 * @param diagnosticReason         diagnostic log or failure description
 * @param durationMillis           elapsed execution time in milliseconds
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public record RollbackExecutionResult(
        String snapshotId,
        ServerBinarySlot restoredSlot,
        boolean rolledBackSuccessfully,
        String diagnosticReason,
        long durationMillis
) {

    public RollbackExecutionResult {
        Objects.requireNonNull(snapshotId, "snapshotId must not be null");
        Objects.requireNonNull(restoredSlot, "restoredSlot must not be null");
        Objects.requireNonNull(diagnosticReason, "diagnosticReason must not be null");
    }

    /**
     * Factory method creating a successful {@link RollbackExecutionResult}.
     *
     * @param snapshotId   restored snapshot id
     * @param restoredSlot restored active slot
     * @param reason       success narrative
     * @param elapsedMs    duration in milliseconds
     * @return populated successful result
     */
    public static RollbackExecutionResult successful(
            String snapshotId,
            ServerBinarySlot restoredSlot,
            String reason,
            long elapsedMs) {
        return new RollbackExecutionResult(snapshotId, restoredSlot, true, reason, elapsedMs);
    }

    /**
     * Factory method creating a failed {@link RollbackExecutionResult}.
     *
     * @param snapshotId     attempted snapshot id
     * @param attemptedSlot  attempted active slot
     * @param failureMessage error narrative
     * @param elapsedMs      duration in milliseconds
     * @return populated failed result
     */
    public static RollbackExecutionResult failed(
            String snapshotId,
            ServerBinarySlot attemptedSlot,
            String failureMessage,
            long elapsedMs) {
        return new RollbackExecutionResult(snapshotId, attemptedSlot, false, failureMessage, elapsedMs);
    }
}
