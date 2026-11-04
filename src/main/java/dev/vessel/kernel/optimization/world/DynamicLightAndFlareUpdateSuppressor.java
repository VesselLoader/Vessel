package dev.vessel.kernel.optimization.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains an ephemeral in-memory light overlay mask with tick hysteresis for mobile
 * and temporary light sources (Torchmaster Feral Flare Lanterns, Ars Nouveau Light
 * projectiles, Create contraption headlamps), preventing continuous chunk disk dirtying
 * and redundant light engine recalculations.
 *
 * <p>Employs {@link DynamicLightSourceState} to consolidate lookups and eliminate redundant hashing.</p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 * @author VESSEL Engine Team
 */
public final class DynamicLightAndFlareUpdateSuppressor {

    private final Map<Long, DynamicLightSourceState> sourceStates = new ConcurrentHashMap<>();
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
        DynamicLightSourceState state = sourceStates.get(entityOrLanternId);
        if (state != null) {
            synchronized (state) {
                if ((currentTick - state.getLastUpdateTick()) < Math.max(1, hysteresisTicks)) {
                    return false;
                }
                long prevPos = state.getLastPos();
                if (prevPos != newBlockPos) {
                    ephemeralLightByBlockPos.remove(prevPos);
                }
                state.setLastPos(newBlockPos);
                state.setLastUpdateTick(currentTick);
            }
        } else {
            sourceStates.put(entityOrLanternId, new DynamicLightSourceState(newBlockPos, currentTick));
        }

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
        Integer lvl = ephemeralLightByBlockPos.get(packedBlockPos);
        return lvl != null ? lvl : 0;
    }

    /**
     * Removes an ephemeral light source when destroyed or despawned.
     *
     * @param entityOrLanternId source identifier
     */
    public void removeEphemeralLightSource(long entityOrLanternId) {
        DynamicLightSourceState state = sourceStates.remove(entityOrLanternId);
        if (state != null) {
            synchronized (state) {
                ephemeralLightByBlockPos.remove(state.getLastPos());
            }
        }
    }

    /**
     * Clears all ephemeral light sources upon dimension unload.
     */
    public void clearAll() {
        sourceStates.clear();
        ephemeralLightByBlockPos.clear();
    }
}
