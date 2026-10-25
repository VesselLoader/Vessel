package dev.vessel.kernel.network.bedrock;

/**
 * 3x3 rotation matrix helper for performing high-precision Euler angle transformations
 * between Java (Z-Y-X intrinsic order) and Bedrock (X-Y-Z extrinsic order) coordinate systems.
 * Pure Java 21 clean-room implementation with zero external dependencies.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class Matrix3fRotationHelper {

    private static final double EPSILON = 1e-6;

    private Matrix3fRotationHelper() {
    }

    /**
     * Converts Euler angles from Java order (Z-Y-X: Roll then Yaw then Pitch)
     * to Bedrock order (X-Y-Z: Pitch then Yaw then Roll) in degrees.
     *
     * @param javaEuler Euler rotation in Java order [pitch(X), yaw(Y), roll(Z)] in degrees
     * @return equivalent Euler rotation in Bedrock order in degrees
     */
    public static EulerRotation3f convertJavaToBedrockOrder(EulerRotation3f javaEuler) {
        double radPitch = Math.toRadians(javaEuler.pitchDegrees()); // X
        double radYaw   = Math.toRadians(javaEuler.yawDegrees());   // Y
        double radRoll  = Math.toRadians(javaEuler.rollDegrees());  // Z

        // 1. Build Java Matrix R = Rz(roll) * Ry(yaw) * Rx(pitch)
        double cz = Math.cos(radRoll);
        double sz = Math.sin(radRoll);
        double cy = Math.cos(radYaw);
        double sy = Math.sin(radYaw);
        double cx = Math.cos(radPitch);
        double sx = Math.sin(radPitch);

        // Matrix entries: R = Rz * Ry * Rx
        double r00 = cz * cy;
        double r01 = (cz * sy * sx) - (sz * cx);
        double r02 = (cz * sy * cx) + (sz * sx);

        double r10 = sz * cy;
        double r11 = (sz * sy * sx) + (cz * cx);
        double r12 = (sz * sy * cx) - (cz * sx);

        double r20 = -sy;
        double r21 = cy * sx;
        double r22 = cy * cx;

        // 2. Decompose Matrix into Bedrock order R = Rx(bx) * Ry(by) * Rz(bz)
        // R = [ cy*cz,          -cy*sz,         sy      ]
        //     [ sx*sy*cz + cx*sz, -sx*sy*sz + cx*cz, -sx*cy ]
        //     [-cx*sy*cz + sx*sz,  cx*sy*sz + sx*cz,  cx*cy ]

        double by; // Yaw
        double bx; // Pitch
        double bz; // Roll

        double sinY = Math.clamp(r02, -1.0, 1.0);
        by = Math.asin(sinY);

        double cosY = Math.cos(by);
        if (Math.abs(cosY) > EPSILON) {
            bx = Math.atan2(-r12, r22);
            bz = Math.atan2(-r01, r00);
        } else {
            // Gimbal lock singularity
            bz = 0.0;
            bx = Math.atan2(r10, r11);
        }

        return new EulerRotation3f(
                (float) Math.toDegrees(bx),
                (float) Math.toDegrees(by),
                (float) Math.toDegrees(bz)
        );
    }
}
