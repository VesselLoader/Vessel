package dev.vessel.kernel.modelengine.lod;

/**
 * Configuration profile for client-side GPU interpolation of Display entities.
 * <p>
 * Controls {@code interpolation_duration} (default 2-3 ticks / 100-150ms) and {@code start_interpolation} (0 ticks),
 * instructing vanilla Minecraft clients to smoothly blend bone translation, rotation, and scale
 * at 60+ FPS without requiring per-frame or high-frequency server packet dispatch.
 * </p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DisplayInterpolationConfig {

    public static final int DEFAULT_INTERPOLATION_DURATION_TICKS = 3;
    public static final int DEFAULT_START_INTERPOLATION_TICKS = 0;
    public static final int DEFAULT_TELEPORT_DURATION_TICKS = 0;

    private int interpolationDurationTicks = DEFAULT_INTERPOLATION_DURATION_TICKS;
    private int startInterpolationTicks = DEFAULT_START_INTERPOLATION_TICKS;
    private int teleportDurationTicks = DEFAULT_TELEPORT_DURATION_TICKS;

    /**
     * Constructs a DisplayInterpolationConfig with default 3-tick (150ms) interpolation.
     */
    public DisplayInterpolationConfig() {
    }

    /**
     * Constructs a DisplayInterpolationConfig with custom interpolation duration.
     *
     * @param interpolationDurationTicks duration in server ticks (e.g. 2 or 3)
     */
    public DisplayInterpolationConfig(int interpolationDurationTicks) {
        setInterpolationDurationTicks(interpolationDurationTicks);
    }

    public int getInterpolationDurationTicks() {
        return interpolationDurationTicks;
    }

    public void setInterpolationDurationTicks(int interpolationDurationTicks) {
        if (interpolationDurationTicks < 0) {
            throw new IllegalArgumentException("interpolationDurationTicks cannot be negative: " + interpolationDurationTicks);
        }
        this.interpolationDurationTicks = interpolationDurationTicks;
    }

    public int getStartInterpolationTicks() {
        return startInterpolationTicks;
    }

    public void setStartInterpolationTicks(int startInterpolationTicks) {
        if (startInterpolationTicks < 0) {
            throw new IllegalArgumentException("startInterpolationTicks cannot be negative: " + startInterpolationTicks);
        }
        this.startInterpolationTicks = startInterpolationTicks;
    }

    public int getTeleportDurationTicks() {
        return teleportDurationTicks;
    }

    public void setTeleportDurationTicks(int teleportDurationTicks) {
        if (teleportDurationTicks < 0) {
            throw new IllegalArgumentException("teleportDurationTicks cannot be negative: " + teleportDurationTicks);
        }
        this.teleportDurationTicks = teleportDurationTicks;
    }

    /**
     * Computes the interpolation duration in milliseconds (50ms per tick).
     *
     * @return duration in milliseconds
     */
    public long getInterpolationDurationMillis() {
        return this.interpolationDurationTicks * 50L;
    }
}
