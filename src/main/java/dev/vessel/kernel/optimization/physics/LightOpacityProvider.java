package dev.vessel.kernel.optimization.physics;

/**
 * Functional interface providing voxel light attenuation/opacity values (0..15).
 * <p>
 * An opacity of 0 represents complete transparency (e.g. air), values from 1 to 14
 * represent partial light attenuation (e.g. water, ice, tinted glass), and 15 represents
 * complete light obstruction (e.g. stone, dirt, solid metal).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface LightOpacityProvider {

    /**
     * Complete transparency opacity constant.
     */
    int TRANSPARENT = 0;

    /**
     * Complete light obstruction opacity constant.
     */
    int OPAQUE = 15;

    /**
     * Returns the light opacity attenuation for the voxel at the given section-local coordinates.
     *
     * @param x section-local X coordinate (0..15)
     * @param y section-local Y coordinate (0..15)
     * @param z section-local Z coordinate (0..15)
     * @return opacity level between 0 and 15 inclusive
     */
    int getOpacity(int x, int y, int z);
}
