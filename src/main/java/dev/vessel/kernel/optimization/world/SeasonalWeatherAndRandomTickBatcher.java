package dev.vessel.kernel.optimization.world;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Batches seasonal snow/ice/foliage transitions (Serene Seasons, Immersive Weathering,
 * Snow! Real Magic!) and accelerated crop randomTicks (Mystical Agriculture) per chunk
 * section into a single multi-block section packet while suppressing purely visual
 * neighbor block updates.
 *
 * <p>Employs {@link SectionBlockChangeBatch} primitive storage to eliminate boxing churn.</p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class SeasonalWeatherAndRandomTickBatcher {

    private final Map<Long, SectionBlockChangeBatch> sectionBatches = new ConcurrentHashMap<>();

    /**
     * Creates a new seasonal weather and randomTick batcher.
     */
    public SeasonalWeatherAndRandomTickBatcher() {
    }

    /**
     * Queues a seasonal or randomTick block state change inside {@code chunkSectionKey}
     * if the current section batch has not reached {@code maxBatchPerTick}.
     *
     * @param chunkSectionKey 64-bit packed chunk section key
     * @param packedLocalPos  12-bit packed section-local position (0..4095)
     * @param newBlockStateId target raw block state ID
     * @param maxBatchPerTick maximum block changes permitted per section per tick
     * @return {@code true} if queued into the current tick's batch, {@code false} if deferred
     */
    public boolean queueSeasonalBlockChange(
            long chunkSectionKey,
            short packedLocalPos,
            int newBlockStateId,
            int maxBatchPerTick) {
        if (maxBatchPerTick <= 0) {
            return false;
        }
        SectionBlockChangeBatch batch =
                sectionBatches.computeIfAbsent(
                        chunkSectionKey, k -> new SectionBlockChangeBatch(maxBatchPerTick));
        return batch.addOrUpdate(packedLocalPos, newBlockStateId, maxBatchPerTick);
    }

    /**
     * Flushes and returns all queued block state changes for {@code chunkSectionKey}
     * so they can be broadcast as a single {@code ClientboundSectionBlocksUpdatePacket}.
     *
     * @param chunkSectionKey 64-bit packed chunk section key
     * @return unmodifiable map of section-local positions to new block state IDs
     */
    public Map<Short, Integer> flushSectionMultiBlockPacket(long chunkSectionKey) {
        SectionBlockChangeBatch batch = sectionBatches.remove(chunkSectionKey);
        if (batch == null || batch.isEmpty()) {
            return Collections.emptyMap();
        }
        return batch.toMap();
    }

    /**
     * Evaluates whether a visual neighbor block update should be suppressed to protect
     * TPS during massive season/snow/ice state changes.
     *
     * @param blockUpdateCause reason identifier for the neighbor update
     * @return {@code true} if the neighbor update should be suppressed
     */
    public boolean shouldSuppressVisualNeighborUpdate(String blockUpdateCause) {
        if (blockUpdateCause == null) {
            return false;
        }
        String lower = blockUpdateCause.trim().toLowerCase(Locale.ROOT);
        return lower.contains("snow_melt")
                || lower.contains("ice_melt")
                || lower.contains("leaf_decay")
                || lower.contains("foliage_tint")
                || lower.contains("weathering");
    }

    /**
     * Returns the total count of chunk sections with pending multi-block update batches.
     *
     * @return pending section batch count
     */
    public int getPendingSectionBatchCount() {
        return sectionBatches.size();
    }
}
