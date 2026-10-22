package dev.vessel.kernel.optimization.block;

/**
 * Fluid simulation type specifications governing tick intervals and maximum flow radius.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum FluidSimulationType {
    WATER(5, 7, 1),
    LAVA_OVERWORLD(30, 3, 2),
    LAVA_NETHER(10, 7, 1);

    private final int tickDelay;
    private final int maxFlowDistance;
    private final int dropPerBlock;

    FluidSimulationType(int tickDelay, int maxFlowDistance, int dropPerBlock) {
        this.tickDelay = tickDelay;
        this.maxFlowDistance = maxFlowDistance;
        this.dropPerBlock = dropPerBlock;
    }

    public int tickDelay() {
        return tickDelay;
    }

    public int maxFlowDistance() {
        return maxFlowDistance;
    }

    public int dropPerBlock() {
        return dropPerBlock;
    }
}
