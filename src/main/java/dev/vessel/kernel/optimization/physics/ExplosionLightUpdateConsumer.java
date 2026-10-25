package dev.vessel.kernel.optimization.physics;

/**
 * Consumer callback for dispatching single batched lighting recalculations per chunk section.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface ExplosionLightUpdateConsumer {

    /**
     * Schedules a lighting recalculation pass for an affected 16x16x16 chunk section.
     *
     * @param sectionX chunk section coordinate X (blockX &gt;&gt; 4)
     * @param sectionY chunk section coordinate Y (blockY &gt;&gt; 4)
     * @param sectionZ chunk section coordinate Z (blockZ &gt;&gt; 4)
     */
    void scheduleSectionRelight(int sectionX, int sectionY, int sectionZ);
}
