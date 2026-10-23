package dev.vessel.kernel.optimization.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Culls redundant industrial machine sound and particle packets (Mekanism Factories,
 * Create Cogwheels/Steam Engines, Immersive Engineering Crushers) using an $8 \times 8 \times 8$
 * spatial acoustic grid and distance-squared culling before Netty packet serialization.
 *
 * @version 1.0.0
 */
public final class SpatialSoundAndParticleAcousticCuller {

    private final Map<String, AtomicInteger> cellSoundCounts = new ConcurrentHashMap<>();

    /**
     * Creates a new spatial sound and particle acoustic culler.
     */
    public SpatialSoundAndParticleAcousticCuller() {
    }

    /**
     * Determines whether a machine sound event at {@code (soundX, soundY, soundZ)} should
     * be broadcast based on nearest player distance and the per-cell duplicate cap inside
     * its $8 \times 8 \times 8$ acoustic cell.
     *
     * @param soundX                  sound block X
     * @param soundY                  sound block Y
     * @param soundZ                  sound block Z
     * @param soundEventId            numeric sound event identifier
     * @param nearestPlayerDistanceSq squared distance to the nearest player
     * @param maxAudibleDistanceSq    maximum audible squared distance (e.g., $64^2 = 4096$)
     * @param maxSameSoundPerCell     maximum identical sounds allowed per $8 \times 8 \times 8$ cell per window
     * @return {@code true} if the sound packet should be broadcast, {@code false} if culled
     */
    public boolean shouldBroadcastMachineSound(
            int soundX,
            int soundY,
            int soundZ,
            int soundEventId,
            double nearestPlayerDistanceSq,
            double maxAudibleDistanceSq,
            int maxSameSoundPerCell) {
        if (nearestPlayerDistanceSq > maxAudibleDistanceSq || maxSameSoundPerCell <= 0) {
            return false;
        }
        int cellX = soundX >> 3;
        int cellY = soundY >> 3;
        int cellZ = soundZ >> 3;
        String cellKey = cellX + ":" + cellY + ":" + cellZ + "#" + soundEventId;
        AtomicInteger counter =
                cellSoundCounts.computeIfAbsent(cellKey, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        return current <= maxSameSoundPerCell;
    }

    /**
     * Resets the acoustic window counters (typically every 10 server ticks).
     */
    public void resetAcousticWindow() {
        cellSoundCounts.clear();
    }
}
