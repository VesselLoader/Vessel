package dev.vessel.kernel.optimization.physics;

/**
 * Standard default opacity provider where all voxels are completely transparent (opacity = 0).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TransparentLightOpacityProvider implements LightOpacityProvider {

    /**
     * Reusable singleton instance.
     */
    public static final TransparentLightOpacityProvider INSTANCE = new TransparentLightOpacityProvider();

    /**
     * Constructs a TransparentLightOpacityProvider.
     */
    public TransparentLightOpacityProvider() {
    }

    @Override
    public int getOpacity(int x, int y, int z) {
        return TRANSPARENT;
    }
}
