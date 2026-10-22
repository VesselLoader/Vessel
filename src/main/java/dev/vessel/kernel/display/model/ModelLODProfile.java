package dev.vessel.kernel.display.model;

/**
 * Level-Of-Detail (LOD) configuration profile governing skeletal animation update rate
 * and geometric detail based on camera distance.
 *
 * @param lodLevel tier index (0 = full detail, 3 = maximum simplification)
 * @param updateIntervalTicks ticks between bone transform updates (1 = 20 TPS, 2 = 10 TPS, 4 = 5 TPS, 20 = 1 TPS)
 * @param meshSimplificationFactor geometric mesh resolution scale (1.0 = original, 0.25 = low-poly proxy)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ModelLODProfile(
        int lodLevel,
        int updateIntervalTicks,
        float meshSimplificationFactor
) {
    public static final ModelLODProfile LOD_0 = new ModelLODProfile(0, 1, 1.0f);
    public static final ModelLODProfile LOD_1 = new ModelLODProfile(1, 2, 0.75f);
    public static final ModelLODProfile LOD_2 = new ModelLODProfile(2, 4, 0.5f);
    public static final ModelLODProfile LOD_3 = new ModelLODProfile(3, 20, 0.25f);

    /**
     * Checks if a model at this LOD tier should transmit bone transformations on the current tick.
     * Staggers updates across models using the modelId as phase offset.
     *
     * @param currentTick current game tick
     * @param modelId unique model identifier
     * @return true if update packet should be broadcast
     */
    public boolean shouldUpdateAnimation(long currentTick, int modelId) {
        if (updateIntervalTicks <= 1) {
            return true;
        }
        return ((currentTick + modelId) % updateIntervalTicks) == 0;
    }
}
