package dev.vessel.kernel.testmatrix;

import java.util.List;
import java.util.Objects;

/**
 * Immutable report summarizing the outcome of a concurrent multi-loader execution harness run.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param successful whether all loaders completed concurrent bootstrap without classpath collision
 * @param totalTargetsTested total number of modules or plugins exercised
 * @param collisionCount number of package or class collisions detected
 * @param conflictDetails detailed diagnostic descriptions of detected collisions
 * @param durationNanos total test execution duration in nanoseconds
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record MultiLoaderTestResult(
        boolean successful,
        int totalTargetsTested,
        int collisionCount,
        List<String> conflictDetails,
        long durationNanos
) {
    public MultiLoaderTestResult {
        Objects.requireNonNull(conflictDetails, "conflictDetails must not be null");
        conflictDetails = List.copyOf(conflictDetails);
    }
}
