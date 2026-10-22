package dev.vessel.kernel.network;

import java.util.Objects;

/**
 * Immutable coalesced particle effect payload suitable for consolidated network broadcast.
 *
 * @param particleType namespaced particle identifier (e.g. "minecraft:smoke")
 * @param x origin coordinate X
 * @param y origin coordinate Y
 * @param z origin coordinate Z
 * @param offsetX random spread offset X
 * @param offsetY random spread offset Y
 * @param offsetZ random spread offset Z
 * @param speed particle movement speed
 * @param count total particle spawn count
 * @param clusterKey packed spatial cluster coordinate key
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ThrottledParticleEffect(
        String particleType,
        double x,
        double y,
        double z,
        float offsetX,
        float offsetY,
        float offsetZ,
        float speed,
        int count,
        long clusterKey
) {
    public ThrottledParticleEffect {
        Objects.requireNonNull(particleType, "particleType cannot be null");
        if (count <= 0) count = 1;
    }

    /**
     * Creates a new instance with an updated count.
     */
    public ThrottledParticleEffect withCount(int newCount) {
        return new ThrottledParticleEffect(particleType, x, y, z, offsetX, offsetY, offsetZ, speed, newCount, clusterKey);
    }
}
