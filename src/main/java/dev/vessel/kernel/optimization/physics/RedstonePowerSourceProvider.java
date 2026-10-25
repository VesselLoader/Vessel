package dev.vessel.kernel.optimization.physics;

/**
 * Accessor interface querying external redstone power sources (levers, redstone torches, repeaters, redstone blocks).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface RedstonePowerSourceProvider {

    /**
     * Minimum possible redstone signal power.
     */
    int MIN_POWER = 0;

    /**
     * Maximum possible redstone signal power in standard Minecraft mechanics.
     */
    int MAX_POWER = 15;

    /**
     * Returns the maximum external redstone power level supplied to the specified wire location (0..15).
     *
     * @param x block world X coordinate
     * @param y block world Y coordinate
     * @param z block world Z coordinate
     * @return power level between 0 and 15 inclusive
     */
    int getExternalPower(int x, int y, int z);
}
