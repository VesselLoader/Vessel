package dev.vessel.kernel.transform.mixin;

import java.util.List;
import java.util.Objects;

/**
 * Immutable outcome of an overwrite arbitration process between conflicting mod mixins.
 *
 * @param targetClass target class internal name
 * @param targetMethod target method name and descriptor
 * @param strategyApplied arbitration strategy employed
 * @param orderedCandidates list of sorted candidate overwrites in execution order
 * @param resolvedSuccessfully true if conflicts were safely mitigated
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ArbitratedOverwriteResolution(
        String targetClass,
        String targetMethod,
        ArbitrationStrategy strategyApplied,
        List<CandidateOverwrite> orderedCandidates,
        boolean resolvedSuccessfully
) {
    public ArbitratedOverwriteResolution {
        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        Objects.requireNonNull(targetMethod, "targetMethod cannot be null");
        Objects.requireNonNull(strategyApplied, "strategyApplied cannot be null");
        orderedCandidates = orderedCandidates == null ? List.of() : List.copyOf(orderedCandidates);
    }
}
