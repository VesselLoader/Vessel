package dev.vessel.kernel.optimization.supermod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Rate-limits expensive block/biome compass and scanner searches (Nature's Compass, Explorer's
 * Compass, Digital Miner, Scannable) to a bounded number of block checks per tick.
 *
 * @version 1.0.0
 */
public final class ChunkBlockSearchRateLimiter {

    /**
     * Evaluates at most {@code maxChecksPerTick} candidate block positions against
     * {@code matchesTargetPredicate} and returns all matching positions within that budget.
     *
     * @param candidatePositions     the list of packed 64-bit candidate block coordinates
     * @param matchesTargetPredicate the predicate testing whether a packed position matches
     * @param maxChecksPerTick       the maximum number of predicate evaluations allowed this tick
     * @return the list of matching packed 64-bit block coordinates found within budget
     */
    public List<Long> executeRateLimitedBlockSearch(
            List<Long> candidatePositions,
            LongPredicate matchesTargetPredicate,
            int maxChecksPerTick) {
        Objects.requireNonNull(candidatePositions, "candidatePositions must not be null");
        Objects.requireNonNull(matchesTargetPredicate, "matchesTargetPredicate must not be null");
        if (maxChecksPerTick <= 0 || candidatePositions.isEmpty()) {
            return List.of();
        }
        int limit = Math.min(maxChecksPerTick, candidatePositions.size());
        List<Long> matches = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Long pos = candidatePositions.get(i);
            if (pos != null && matchesTargetPredicate.test(pos)) {
                matches.add(pos);
            }
        }
        return List.copyOf(matches);
    }
}
