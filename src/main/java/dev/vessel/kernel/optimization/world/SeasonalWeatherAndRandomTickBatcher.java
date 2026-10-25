package dev.vessel.kernel.optimization.world;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Batches seasonal snow/ice/foliage transitions (Serene Seasons, Immersive Weathering,
 * Snow! Real Magic!) and accelerated crop randomTicks (Mystical Agriculture) per chunk
 * section into a single multi-block section packet while suppressing purely visual
 * neighbor block updates.
 *
 * @version 1.0.0
 */
public final class SeasonalWeatherAndRandomTickBatcher {

    private final Map<Long, Map<Short, Integer>> sectionBatches = new ConcurrentHashMap<>();

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
        Map<Short, Integer> batch =
                sectionBatches.computeIfAbsent(
                        chunkSectionKey, k -> new ConcurrentHashMap<>());
        synchronized (batch) {
            if (!batch.containsKey(packedLocalPos) && batch.size() >= maxBatchPerTick) {
                return false;
            }
            batch.put(packedLocalPos, newBlockStateId);
            return true;
        }
    }

    /**
     * Flushes and returns all queued block state changes for {@code chunkSectionKey}
     * so they can be broadcast as a single {@code ClientboundSectionBlocksUpdatePacket}.
     *
     * @param chunkSectionKey 64-bit packed chunk section key
     * @return unmodifiable map of section-local positions to new block state IDs
     */
    public Map<Short, Integer> flushSectionMultiBlockPacket(long chunkSectionKey) {
        Map<Short, Integer> removed = sectionBatches.remove(chunkSectionKey);
        if (removed == null || removed.isEmpty()) {
            return Collections.emptyMap();
        }
        synchronized (removed) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(removed));
        }
    }

    /**
     * Determines whether 6-way neighbor block updates should be suppressed because the
     * block state transition is purely cosmetic/seasonal (snow layer melt, ice thaw,
     * leaf color shift, moss weathering).
     *
     * @param blockChangeReason descriptor of the block state change reason
     * @return {@code true} if neighbor updates should be skipped
     */
    public boolean shouldSuppressVisualNeighborUpdate(String blockChangeReason) {
        Objects.requireNonNull(blockChangeReason, "blockChangeReason must not be null");
        String normalized = blockChangeReason.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("seasonal")
                || normalized.contains("snow")
                || normalized.contains("thaw")
                || normalized.contains("weathering")
                || normalized.contains("foliage_tint");
    }
}
