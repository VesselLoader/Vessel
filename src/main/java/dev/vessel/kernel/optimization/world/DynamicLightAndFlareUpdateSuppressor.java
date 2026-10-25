package dev.vessel.kernel.optimization.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains an ephemeral in-memory light overlay mask with tick hysteresis for mobile
 * and temporary light sources (Torchmaster Feral Flare Lanterns, Ars Nouveau Light
 * projectiles, Create contraption headlamps), preventing continuous chunk disk dirtying
 * and redundant light engine recalculations.
 *
 * @version 1.0.0
 */
public final class DynamicLightAndFlareUpdateSuppressor {

    private final Map<Long, Long> lastPosBySourceId = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastUpdateTickBySourceId = new ConcurrentHashMap<>();
    private final Map<Long, Integer> ephemeralLightByBlockPos = new ConcurrentHashMap<>();

    /**
     * Creates a new dynamic light and flare update suppressor.
     */
    public DynamicLightAndFlareUpdateSuppressor() {
    }

    /**
     * Updates the ephemeral light position of a moving entity or flare lantern if at
     * least {@code hysteresisTicks} have elapsed since its last position update.
     *
     * @param entityOrLanternId unique entity or lantern identifier
     * @param newBlockPos       64-bit packed block position
     * @param luminanceLevel    emitted block light level (0..15)
     * @param currentTick       current server tick
     * @param hysteresisTicks   minimum ticks between position updates (e.g., 10)
     * @return {@code true} if the ephemeral light mask was updated, {@code false} if throttled
     */
    public boolean updateEphemeralLightSource(
            long entityOrLanternId,
            long newBlockPos,
            int luminanceLevel,
            long currentTick,
            int hysteresisTicks) {
        Long lastTick = lastUpdateTickBySourceId.get(entityOrLanternId);
        if (lastTick != null && (currentTick - lastTick) < Math.max(1, hysteresisTicks)) {
            return false;
        }
        Long previousPos = lastPosBySourceId.put(entityOrLanternId, newBlockPos);
        if (previousPos != null && previousPos != newBlockPos) {
            ephemeralLightByBlockPos.remove(previousPos);
        }
        lastUpdateTickBySourceId.put(entityOrLanternId, currentTick);
        int clampedLight = Math.max(0, Math.min(15, luminanceLevel));
        if (clampedLight > 0) {
            ephemeralLightByBlockPos.put(newBlockPos, clampedLight);
        } else {
            ephemeralLightByBlockPos.remove(newBlockPos);
        }
        return true;
    }

    /**
     * Returns the current ephemeral in-memory light level at {@code packedBlockPos}.
     *
     * @param packedBlockPos 64-bit packed block position
     * @return ephemeral light level (0..15)
     */
    public int getEphemeralLightLevel(long packedBlockPos) {
        return ephemeralLightByBlockPos.getOrDefault(packedBlockPos, 0);
    }
}
