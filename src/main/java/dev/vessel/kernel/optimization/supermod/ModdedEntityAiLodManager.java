package dev.vessel.kernel.optimization.supermod;

/**
 * Distance-tiered Level-of-Detail (LOD) AI scheduler for modded entities (Alex's Mobs, Cobblemon,
 * Ice &amp; Fire). Full rate inside {@code nearRadius}, quarter rate inside {@code midRadius}, and
 * dormant or 1/20th rate beyond {@code midRadius}.
 *
 * @version 1.0.0
 */
public final class ModdedEntityAiLodManager {

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
        double nearSq = nearRadius * nearRadius;
        if (distanceSqToNearestPlayer <= nearSq) {
            return true;
        }
        double midSq = midRadius * midRadius;
        if (distanceSqToNearestPlayer <= midSq) {
            return (currentTick % 4L) == 0L;
        }
        if (dormantWhenFar) {
            return false;
        }
        return (currentTick % 20L) == 0L;
    }
}
