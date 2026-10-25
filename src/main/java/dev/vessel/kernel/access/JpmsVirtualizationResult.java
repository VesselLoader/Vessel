package dev.vessel.kernel.access;

import java.util.Objects;

/**
 * Immutable carrier reporting the result of a JPMS module boundary virtualization attempt.
 *
 * @param sourceModule name of the module containing the package
 * @param packageName name of the package being opened
 * @param targetModule name of the module receiving access (or "ALL-UNNAMED")
 * @param successful true if the package was successfully opened or was already accessible
 * @param diagnosticMessage explanation or error detail
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record JpmsVirtualizationResult(
        String sourceModule,
        String packageName,
        String targetModule,
        boolean successful,
        String diagnosticMessage
) {
    public JpmsVirtualizationResult {
        Objects.requireNonNull(sourceModule, "sourceModule cannot be null");
        Objects.requireNonNull(packageName, "packageName cannot be null");
        Objects.requireNonNull(targetModule, "targetModule cannot be null");
        Objects.requireNonNull(diagnosticMessage, "diagnosticMessage cannot be null");
    }
}
