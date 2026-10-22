package dev.vessel.kernel.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Particle broadcast throttle and spatial coalescing engine.
 * Groups particle emissions into spatial 4x4x4 block clusters, clamps maximum particle
 * density per cluster per tick, and coalesces duplicate particles into single aggregated
 * network frames to prevent client FPS drops and network socket saturation.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ParticleBroadcastThrottle {

    public static final int DEFAULT_MAX_PARTICLES_PER_CLUSTER = 64;

    private final int maxParticlesPerCluster;
    private final Map<Long, Map<String, ThrottledParticleEffect>> clusterMap = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> clusterCounts = new ConcurrentHashMap<>();
    private final AtomicLong droppedParticlesCount = new AtomicLong(0L);

    /**
     * Constructs a ParticleBroadcastThrottle with default max particles limit (64).
     */
    public ParticleBroadcastThrottle() {
        this(DEFAULT_MAX_PARTICLES_PER_CLUSTER);
    }

    /**
     * Constructs a ParticleBroadcastThrottle with specified per-cluster limit.
     *
     * @param maxParticlesPerCluster maximum particle instances allowed per 4x4x4 cluster per tick
     */
    public ParticleBroadcastThrottle(int maxParticlesPerCluster) {
        this.maxParticlesPerCluster = Math.max(1, maxParticlesPerCluster);
    }

    /**
     * Submits a particle emission to the throttler.
     *
     * @param type namespaced particle type
     * @param x coordinate X
     * @param y coordinate Y
     * @param z coordinate Z
     * @param offsetX spread offset X
     * @param offsetY spread offset Y
     * @param offsetZ spread offset Z
     * @param speed particle velocity
     * @param count count of particles
     * @return true if accepted, false if dropped due to cluster saturation
     */
    public boolean addParticle(
            String type,
            double x,
            double y,
            double z,
            float offsetX,
            float offsetY,
            float offsetZ,
            float speed,
            int count) {
        Objects.requireNonNull(type, "type cannot be null");
        int safeCount = Math.max(1, count);
        long clusterKey = packClusterKey((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));

        AtomicInteger current = clusterCounts.computeIfAbsent(clusterKey, k -> new AtomicInteger(0));
        int currentTotal = current.get();

        if (currentTotal >= maxParticlesPerCluster) {
            droppedParticlesCount.addAndGet(safeCount);
            return false;
        }

        int allowed = Math.min(safeCount, maxParticlesPerCluster - currentTotal);
        current.addAndGet(allowed);

        if (allowed < safeCount) {
            droppedParticlesCount.addAndGet(safeCount - allowed);
        }

        Map<String, ThrottledParticleEffect> typeMap =
                clusterMap.computeIfAbsent(clusterKey, k -> new ConcurrentHashMap<>());

        typeMap.compute(type, (k, existing) -> {
            if (existing == null) {
                return new ThrottledParticleEffect(type, x, y, z, offsetX, offsetY, offsetZ, speed, allowed, clusterKey);
            } else {
                return existing.withCount(existing.count() + allowed);
            }
        });

        return true;
    }

    /**
     * Flushes and coalesces all accepted particles into a single batch, resetting cluster limits.
     *
     * @return list of consolidated particle effects
     */
    public List<ThrottledParticleEffect> flushBatch() {
        List<ThrottledParticleEffect> batch = new ArrayList<>();
        for (Map<String, ThrottledParticleEffect> typeMap : clusterMap.values()) {
            batch.addAll(typeMap.values());
        }
        clusterMap.clear();
        clusterCounts.clear();
        return batch;
    }

    /**
     * Packs block coordinates into a 4x4x4 cluster key.
     */
    public static long packClusterKey(int bx, int by, int bz) {
        long cx = (bx >> 2) & 0x1FFFFFL;
        long cy = (by >> 2) & 0xFFFL;
        long cz = (bz >> 2) & 0x1FFFFFL;
        return (cx << 33) | (cy << 21) | cz;
    }

    /**
     * Returns total number of particles dropped due to throttling.
     */
    public long getDroppedParticlesCount() {
        return droppedParticlesCount.get();
    }
}
