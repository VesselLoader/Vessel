package dev.vessel.kernel.testmatrix;

import java.util.Objects;

/**
 * Immutable descriptor for an executable method or task target prioritized for JIT compilation warm-up.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param targetIdentifier diagnostic name or signature of the hot method
 * @param tier compilation urgency tier
 * @param warmupThreshold invocation count necessary to trigger compilation
 * @param warmupRunnable executable warm-up invocation payload
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record CompilationTarget(
        String targetIdentifier,
        CompilationTier tier,
        int warmupThreshold,
        Runnable warmupRunnable
) implements Comparable<CompilationTarget> {

    public CompilationTarget {
        Objects.requireNonNull(targetIdentifier, "targetIdentifier must not be null");
        Objects.requireNonNull(tier, "tier must not be null");
        Objects.requireNonNull(warmupRunnable, "warmupRunnable must not be null");
        if (warmupThreshold <= 0) {
            throw new IllegalArgumentException("warmupThreshold must be positive");
        }
    }

    @Override
    public int compareTo(CompilationTarget other) {
        int cmp = Integer.compare(this.tier.priorityOrder(), other.tier.priorityOrder());
        if (cmp != 0) {
            return cmp;
        }
        return this.targetIdentifier.compareTo(other.targetIdentifier);
    }
}
