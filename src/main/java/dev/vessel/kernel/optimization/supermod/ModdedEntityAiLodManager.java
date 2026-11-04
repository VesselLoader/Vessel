package dev.vessel.kernel.optimization.supermod;

/**
 * Distance-tiered Level-of-Detail (LOD) AI scheduler for modded entities (Alex's Mobs, Cobblemon,
 * Ice &amp; Fire). Full rate inside {@code nearRadius}, quarter rate inside {@code midRadius}, and
 * dormant or 1/20th rate beyond {@code midRadius}.
 *
 * @version 1.0.1
 */
public final class ModdedEntityAiLodManager {

    /**
     * Evaluates whether a modded entity's AI brain/goal selector should tick on the current tick,
     * staggering execution across ticks using entity ID to eliminate thundering-herd lag spikes.
     *
     * @param entityId                  the unique entity identifier
     * @param distanceSqToNearestPlayer squared Euclidean distance to the nearest online player
     * @param currentTick               the current monotonic server tick
     * @param nearRadius                full-rate radius in blocks
     * @param midRadius                 reduced-rate radius in blocks
     * @param dormantWhenFar            whether AI should completely sleep beyond {@code midRadius}
     * @return {@code true} if the entity's AI should execute this tick
     */
    public boolean shouldTickModdedEntityAi(
            int entityId,
            double distanceSqToNearestPlayer,
            long currentTick,
            double nearRadius,
            double midRadius,
            boolean dormantWhenFar) {
        double nearSq = nearRadius * nearRadius;
        if (distanceSqToNearestPlayer <= nearSq) {
            return true;
        }
        double midSq = midRadius * midRadius;
        long staggeredTick = currentTick + (entityId & 0x7FFF);
        if (distanceSqToNearestPlayer <= midSq) {
            return (staggeredTick % 4L) == 0L;
        }
        if (dormantWhenFar) {
            return false;
        }
        return (staggeredTick % 20L) == 0L;
    }

    /**
     * Evaluates whether a modded entity's AI brain/goal selector should tick on the current tick.
     *
     * @param distanceSqToNearestPlayer squared Euclidean distance to the nearest online player
     * @param currentTick               the current monotonic server tick
     * @param nearRadius                full-rate radius in blocks
     * @param midRadius                 reduced-rate radius in blocks
     * @param dormantWhenFar            whether AI should completely sleep beyond {@code midRadius}
     * @return {@code true} if the entity's AI should execute this tick
     */
    public boolean shouldTickModdedEntityAi(
            double distanceSqToNearestPlayer,
            long currentTick,
            double nearRadius,
            double midRadius,
            boolean dormantWhenFar) {
        return shouldTickModdedEntityAi(0, distanceSqToNearestPlayer, currentTick, nearRadius, midRadius, dormantWhenFar);
    }
}
