package dev.vessel.kernel.autoupdate;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable record capturing the result of applying a cumulative patch sequence to a base server jar.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param initialRevision    the revision number prior to applying patches
 * @param finalRevision      the final revision number achieved after rollforward
 * @param appliedPatchCount  total number of individual patches consolidated and applied
 * @param outputJarPath      path to the patched resulting jar artifact
 * @param success            {@code true} if all patches were successfully applied and verified
 * @param summaryMessage     human-readable summary or failure reason
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public record RollforwardApplicationResult(
        int initialRevision,
        int finalRevision,
        int appliedPatchCount,
        Path outputJarPath,
        boolean success,
        String summaryMessage
) {

    public RollforwardApplicationResult {
        Objects.requireNonNull(outputJarPath, "outputJarPath must not be null");
        Objects.requireNonNull(summaryMessage, "summaryMessage must not be null");
    }

    /**
     * Factory method creating a successful {@link RollforwardApplicationResult}.
     *
     * @param initialRevision   starting revision
     * @param finalRevision     ending revision
     * @param appliedPatchCount count of patches applied
     * @param outputJarPath     resulting jar path
     * @param summary           description of changes applied
     * @return successful result instance
     */
    public static RollforwardApplicationResult successful(
            int initialRevision,
            int finalRevision,
            int appliedPatchCount,
            Path outputJarPath,
            String summary) {
        return new RollforwardApplicationResult(
                initialRevision, finalRevision, appliedPatchCount, outputJarPath, true, summary);
    }

    /**
     * Factory method creating a failed {@link RollforwardApplicationResult}.
     *
     * @param initialRevision starting revision
     * @param attemptedRevision revision attempted to reach
     * @param outputJarPath   attempted target path
     * @param errorMessage    failure explanation
     * @return failed result instance
     */
    public static RollforwardApplicationResult failed(
            int initialRevision,
            int attemptedRevision,
            Path outputJarPath,
            String errorMessage) {
        return new RollforwardApplicationResult(
                initialRevision, attemptedRevision, 0, outputJarPath, false, errorMessage);
    }
}
