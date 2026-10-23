package dev.vessel.kernel.network.bridge;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coalesces per-bone skeletal animation transform updates (GeckoLib, Epic Fight, L_Ender's
 * Cataclysm, AzureLib) within a tick into a single batched frame packet per animated entity.
 *
 * @version 1.0.0
 */
public final class AnimationPacketCoalescer {

    private final Map<Integer, Map<String, float[]>> pendingEntityBoneFrames =
            new ConcurrentHashMap<>();

    /**
     * Queues or overwrites the latest bone rotation transform for an animated entity during the
     * current tick.
     *
     * @param entityId the network entity ID
     * @param boneName the skeletal bone identifier
     * @param pitch    the bone pitch rotation in degrees
     * @param yaw      the bone yaw rotation in degrees
     * @param roll     the bone roll rotation in degrees
     */
    public void queueBoneTransformUpdate(
            int entityId,
            String boneName,
            float pitch,
            float yaw,
            float roll) {
        Objects.requireNonNull(boneName, "boneName must not be null");
        pendingEntityBoneFrames
                .computeIfAbsent(entityId, ignored -> new ConcurrentHashMap<>())
                .put(boneName, new float[]{pitch, yaw, roll});
    }

    /**
     * Atomically flushes and returns all coalesced bone transforms for the specified entity as a
     * single frame map.
     *
     * @param entityId the network entity ID
     * @return an immutable map from bone name to {@code float[]{pitch, yaw, roll}}
     */
    public Map<String, float[]> flushCoalescedFrame(int entityId) {
        Map<String, float[]> frame = pendingEntityBoneFrames.remove(entityId);
        if (frame == null || frame.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(frame);
    }
}
