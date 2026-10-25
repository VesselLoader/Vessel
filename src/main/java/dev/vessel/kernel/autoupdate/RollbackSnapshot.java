package dev.vessel.kernel.autoupdate;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot capturing pre-upgrade state, binary backup artifacts, and configuration metadata.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param snapshotId        unique snapshot identifier
 * @param timestampMillis   epoch millisecond timestamp when snapshot was captured
 * @param snapshotSlot      the active binary slot at snapshot creation time
 * @param previousVersion   version string prior to update
 * @param backupBinaryPath  path to the preserved backup binary jar
 * @param metadata          supplementary contextual attributes
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public record RollbackSnapshot(
        String snapshotId,
        long timestampMillis,
        ServerBinarySlot snapshotSlot,
        String previousVersion,
        Path backupBinaryPath,
        Map<String, String> metadata
) {

    public RollbackSnapshot {
        Objects.requireNonNull(snapshotId, "snapshotId must not be null");
        Objects.requireNonNull(snapshotSlot, "snapshotSlot must not be null");
        Objects.requireNonNull(previousVersion, "previousVersion must not be null");
        Objects.requireNonNull(backupBinaryPath, "backupBinaryPath must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");

        metadata = Collections.unmodifiableMap(metadata);
    }
}
