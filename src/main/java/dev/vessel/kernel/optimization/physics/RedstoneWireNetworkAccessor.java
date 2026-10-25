package dev.vessel.kernel.optimization.physics;

/**
 * Accessor interface for reading and writing redstone wire network states during BFS calculation.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public interface RedstoneWireNetworkAccessor {

    /**
     * Checks if a redstone wire is present at the target coordinate.
     *
     * @param x block world X coordinate
     * @param y block world Y coordinate
     * @param z block world Z coordinate
     * @return true if wire is present at this coordinate
     */
    boolean isWire(int x, int y, int z);

    /**
     * Retrieves the current redstone wire power signal (0..15).
     *
     * @param x block world X coordinate
     * @param y block world Y coordinate
     * @param z block world Z coordinate
     * @return current power level (0..15)
     */
    int getWirePower(int x, int y, int z);

    /**
     * Updates the redstone wire power signal at the target location.
     *
     * @param x     block world X coordinate
     * @param y     block world Y coordinate
     * @param z     block world Z coordinate
     * @param power new power level (0..15)
     */
    void setWirePower(int x, int y, int z, int power);
}
