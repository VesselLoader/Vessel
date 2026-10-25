package dev.vessel.kernel.testmatrix;

/**
 * Immutable configuration parameters for orchestrating concurrent virtual bot stress load testing.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param botCount number of concurrent virtual player bots simulated
 * @param ticksToRun total simulated server ticks to execute
 * @param packetsPerTick number of packets each bot transmits per tick
 * @param movementSpeed simulated displacement delta per movement packet
 * @param targetTps target ticks per second expected
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record VirtualBotConfig(
        int botCount,
        int ticksToRun,
        int packetsPerTick,
        double movementSpeed,
        double targetTps
) {
    public static final int DEFAULT_BOT_COUNT = 100;
    public static final int DEFAULT_TICKS = 20;
    public static final int DEFAULT_PACKETS_PER_TICK = 5;
    public static final double DEFAULT_SPEED = 0.25;
    public static final double DEFAULT_TARGET_TPS = 20.0;

    public static VirtualBotConfig createDefault() {
        return new VirtualBotConfig(DEFAULT_BOT_COUNT, DEFAULT_TICKS, DEFAULT_PACKETS_PER_TICK, DEFAULT_SPEED, DEFAULT_TARGET_TPS);
    }

    public VirtualBotConfig {
        if (botCount <= 0) {
            throw new IllegalArgumentException("botCount must be positive");
        }
        if (ticksToRun <= 0) {
            throw new IllegalArgumentException("ticksToRun must be positive");
        }
        if (packetsPerTick <= 0) {
            throw new IllegalArgumentException("packetsPerTick must be positive");
        }
        if (targetTps <= 0.0) {
            throw new IllegalArgumentException("targetTps must be positive");
        }
    }
}
