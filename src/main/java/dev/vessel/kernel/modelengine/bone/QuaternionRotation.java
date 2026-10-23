package dev.vessel.kernel.modelengine.bone;

import java.util.Objects;

/**
 * 3D Quaternion representation (x, y, z, w) for smooth skeletal animation rotations
 * and vanilla Display Entity transformation matrices.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class QuaternionRotation {

    public static final QuaternionRotation IDENTITY = new QuaternionRotation(0.0f, 0.0f, 0.0f, 1.0f);

    private final float x;
    private final float y;
    private final float z;
    private final float w;

    /**
     * Constructs a QuaternionRotation.
     *
     * @param x X component
     * @param y Y component
     * @param z Z component
     * @param w W component
     */
    public QuaternionRotation(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * Creates a quaternion from Euler angles (pitch, yaw, roll) in radians.
     *
     * @param pitch rotation around X axis (radians)
     * @param yaw   rotation around Y axis (radians)
     * @param roll  rotation around Z axis (radians)
     * @return normalized rotation quaternion
     */
    public static QuaternionRotation fromEuler(float pitch, float yaw, float roll) {
        float halfPitch = pitch * 0.5f;
        float halfYaw = yaw * 0.5f;
        float halfRoll = roll * 0.5f;

        float cx = (float) Math.cos(halfPitch);
        float sx = (float) Math.sin(halfPitch);
        float cy = (float) Math.cos(halfYaw);
        float sy = (float) Math.sin(halfYaw);
        float cz = (float) Math.cos(halfRoll);
        float sz = (float) Math.sin(halfRoll);

        float w = cy * cx * cz + sy * sx * sz;
        float x = cy * sx * cz + sy * cx * sz;
        float y = sy * cx * cz - cy * sx * sz;
        float z = cy * cx * sz - sy * sx * cz;

        return normalize(x, y, z, w);
    }

    /**
     * Normalizes the quaternion components.
     */
    public static QuaternionRotation normalize(float x, float y, float z, float w) {
        float lenSq = x * x + y * y + z * z + w * w;
        if (lenSq < 1e-8f) {
            return IDENTITY;
        }
        float invLen = 1.0f / (float) Math.sqrt(lenSq);
        return new QuaternionRotation(x * invLen, y * invLen, z * invLen, w * invLen);
    }

    /**
     * Multiplies this quaternion by another quaternion (this * other).
     *
     * @param other second quaternion
     * @return composite rotation quaternion
     */
    public QuaternionRotation multiply(QuaternionRotation other) {
        Objects.requireNonNull(other, "other cannot be null");
        float newW = this.w * other.w - this.x * other.x - this.y * other.y - this.z * other.z;
        float newX = this.w * other.x + this.x * other.w + this.y * other.z - this.z * other.y;
        float newY = this.w * other.y - this.x * other.z + this.y * other.w + this.z * other.x;
        float newZ = this.w * other.z + this.x * other.y - this.y * other.x + this.z * other.w;
        return normalize(newX, newY, newZ, newW);
    }

    /**
     * Rotates a 3D vector by this quaternion.
     *
     * @param vx vector X
     * @param vy vector Y
     * @param vz vector Z
     * @return rotated 3D float array [rx, ry, rz]
     */
    public float[] rotateVector(float vx, float vy, float vz) {
        // v' = v + 2 * cross(q.xyz, cross(q.xyz, v) + q.w * v)
        float tx = 2.0f * (this.y * vz - this.z * vy);
        float ty = 2.0f * (this.z * vx - this.x * vz);
        float tz = 2.0f * (this.x * vy - this.y * vx);

        float rx = vx + this.w * tx + (this.y * tz - this.z * ty);
        float ry = vy + this.w * ty + (this.z * tx - this.x * tz);
        float rz = vz + this.w * tz + (this.x * ty - this.y * tx);

        return new float[]{rx, ry, rz};
    }

    /**
     * Converts this quaternion back into Euler angles [pitch, yaw, roll] in radians.
     *
     * @return 3-element float array of Euler angles in radians
     */
    public float[] toEulerAngles() {
        // Roll (z-axis rotation)
        float sinrCosp = 2.0f * (w * z + x * y);
        float cosrCosp = 1.0f - 2.0f * (y * y + z * z);
        float roll = (float) Math.atan2(sinrCosp, cosrCosp);

        // Pitch (x-axis rotation)
        float sinp = 2.0f * (w * x - y * z);
        float pitch;
        if (Math.abs(sinp) >= 1.0f) {
            pitch = (float) Math.copySign(Math.PI / 2, sinp); // Use 90 degrees if out of range
        } else {
            pitch = (float) Math.asin(sinp);
        }

        // Yaw (y-axis rotation)
        float sinyCosp = 2.0f * (w * y + z * x);
        float cosyCosp = 1.0f - 2.0f * (x * x + y * y);
        float yaw = (float) Math.atan2(sinyCosp, cosyCosp);

        return new float[]{pitch, yaw, roll};
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getW() {
        return w;
    }
}
