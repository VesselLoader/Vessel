package dev.vessel.kernel.display.model;

/**
 * Immutable parameters controlling 60 FPS surface wave oscillation dynamics
 * for virtual fluid block display entities.
 *
 * @param amplitude height deviation in meters (Minecraft blocks)
 * @param frequency oscillation angular frequency in radians per tick
 * @param pitchRollWobble angular tilt magnitude in radians
 * @param dampingFactor damping factor for surface stabilization [0.0, 1.0]
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record FluidSurfaceWaveParameters(
        float amplitude,
        float frequency,
        float pitchRollWobble,
        float dampingFactor
) {
    public FluidSurfaceWaveParameters {
        if (amplitude < 0.0f) {
            throw new IllegalArgumentException("amplitude must be non-negative: " + amplitude);
        }
        if (frequency < 0.0f) {
            throw new IllegalArgumentException("frequency must be non-negative: " + frequency);
        }
    }

    /**
     * Default gentle wave parameters suitable for water or blood vats.
     */
    public static final FluidSurfaceWaveParameters GENTLE_RIPPLE =
            new FluidSurfaceWaveParameters(0.015f, 0.12f, 0.02f, 0.98f);

    /**
     * Boiling/violent oscillation parameters.
     */
    public static final FluidSurfaceWaveParameters BOILING_TURBULENCE =
            new FluidSurfaceWaveParameters(0.045f, 0.35f, 0.06f, 0.92f);
}
