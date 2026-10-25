package dev.vessel.kernel.network.bedrock;

/**
 * Immutable 3D Euler angles representation in degrees.
 *
 * @param pitchDegrees rotation around X-axis in degrees
 * @param yawDegrees rotation around Y-axis in degrees
 * @param rollDegrees rotation around Z-axis in degrees
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record EulerRotation3f(
        float pitchDegrees,
        float yawDegrees,
        float rollDegrees
) {
    public static final EulerRotation3f ZERO = new EulerRotation3f(0.0f, 0.0f, 0.0f);
}
