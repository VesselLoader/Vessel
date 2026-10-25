package dev.vessel.kernel.modelengine.lod;

/**
 * High-performance delta threshold culling filter.
 * Suppresses redundant bone recalculations and network packet dispatches whenever entity movement
 * delta is below 0.05 blocks and rotation delta is below 0.02 radians (~1.14 degrees).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MovementDeltaCullingFilter {

    public static final double DEFAULT_MOVEMENT_THRESHOLD = 0.05; // 0.05 blocks
    public static final float DEFAULT_ROTATION_THRESHOLD_RADIANS = 0.02f; // ~1.14 degrees

    private final double movementThresholdSq;
    private final float rotationThresholdRadians;

    private double prevX = Double.NaN;
    private double prevY = Double.NaN;
    private double prevZ = Double.NaN;

    private float prevYaw = Float.NaN;
    private float prevPitch = Float.NaN;

    private long culledUpdatesCount = 0;
    private long acceptedUpdatesCount = 0;

    /**
     * Constructs a MovementDeltaCullingFilter with standard thresholds (0.05 blocks, 0.02 radians).
     */
    public MovementDeltaCullingFilter() {
        this(DEFAULT_MOVEMENT_THRESHOLD, DEFAULT_ROTATION_THRESHOLD_RADIANS);
    }

    /**
     * Constructs a MovementDeltaCullingFilter with custom thresholds.
     *
     * @param movementThreshold        movement distance threshold in blocks
     * @param rotationThresholdRadians rotation angle threshold in radians
     */
    public MovementDeltaCullingFilter(double movementThreshold, float rotationThresholdRadians) {
        if (movementThreshold < 0.0) {
            throw new IllegalArgumentException("movementThreshold cannot be negative: " + movementThreshold);
        }
        if (rotationThresholdRadians < 0.0f) {
            throw new IllegalArgumentException("rotationThresholdRadians cannot be negative: " + rotationThresholdRadians);
        }
        this.movementThresholdSq = movementThreshold * movementThreshold;
        this.rotationThresholdRadians = rotationThresholdRadians;
    }

    /**
     * Evaluates whether entity movement or rotation exceeds the thresholds to justify an update.
     *
     * @param currentX     current X position
     * @param currentY     current Y position
     * @param currentZ     current Z position
     * @param currentYaw   current yaw in degrees
     * @param currentPitch current pitch in degrees
     * @return true if update should proceed, false if culled
     */
    public boolean shouldDispatchUpdate(
            double currentX, double currentY, double currentZ,
            float currentYaw, float currentPitch
    ) {
        if (Double.isNaN(prevX)) {
            // First time seen, must dispatch
            accept(currentX, currentY, currentZ, currentYaw, currentPitch);
            return true;
        }

        double dx = currentX - prevX;
        double dy = currentY - prevY;
        double dz = currentZ - prevZ;
        double moveDistSq = dx * dx + dy * dy + dz * dz;

        if (moveDistSq >= movementThresholdSq) {
            accept(currentX, currentY, currentZ, currentYaw, currentPitch);
            return true;
        }

        float deltaYawRad = (float) Math.abs(Math.toRadians(normalizeAngle(currentYaw - prevYaw)));
        float deltaPitchRad = (float) Math.abs(Math.toRadians(normalizeAngle(currentPitch - prevPitch)));

        if (deltaYawRad >= rotationThresholdRadians || deltaPitchRad >= rotationThresholdRadians) {
            accept(currentX, currentY, currentZ, currentYaw, currentPitch);
            return true;
        }

        culledUpdatesCount++;
        return false;
    }

    /**
     * Forces an update and records current coordinates.
     */
    public void forceDispatch(
            double currentX, double currentY, double currentZ,
            float currentYaw, float currentPitch
    ) {
        accept(currentX, currentY, currentZ, currentYaw, currentPitch);
    }

    private void accept(double x, double y, double z, float yaw, float pitch) {
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.prevYaw = yaw;
        this.prevPitch = pitch;
        this.acceptedUpdatesCount++;
    }

    private float normalizeAngle(float angle) {
        float normalized = angle % 360.0f;
        if (normalized >= 180.0f) {
            normalized -= 360.0f;
        }
        if (normalized < -180.0f) {
            normalized += 360.0f;
        }
        return normalized;
    }

    public void reset() {
        this.prevX = Double.NaN;
        this.prevY = Double.NaN;
        this.prevZ = Double.NaN;
        this.prevYaw = Float.NaN;
        this.prevPitch = Float.NaN;
    }

    public long getCulledUpdatesCount() {
        return culledUpdatesCount;
    }

    public long getAcceptedUpdatesCount() {
        return acceptedUpdatesCount;
    }

    public double getMovementThresholdSq() {
        return movementThresholdSq;
    }

    public float getRotationThresholdRadians() {
        return rotationThresholdRadians;
    }
}
