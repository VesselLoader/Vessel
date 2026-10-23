package dev.vessel.kernel.modelengine.driver;

import java.util.Objects;

/**
 * Enforces invisible (Invisible: 1b) and silent (Silent: 1b) properties on the driver entity.
 * Suppresses default mob rendering and sounds so only the custom Model Engine resourcepack
 * model and audio play, while preserving full vanilla AI pathfinding, goals, and collisions.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DriverInvisibilityGovernor {

    public static final byte METADATA_FLAG_INVISIBLE = 0x20;

    private boolean enforceInvisibility = true;
    private boolean enforceSilence = true;

    /**
     * Constructs a DriverInvisibilityGovernor with default settings.
     */
    public DriverInvisibilityGovernor() {
    }

    /**
     * Applies invisibility and silence flags to the given driver state.
     *
     * @param state driver mob state
     */
    public void governDriver(DriverMobState state) {
        Objects.requireNonNull(state, "state cannot be null");
        if (enforceInvisibility) {
            state.setInvisible(true);
        }
        if (enforceSilence) {
            state.setSilent(true);
        }
    }

    /**
     * Computes the entity data watcher byte mask including invisibility if enforced.
     *
     * @param existingMask existing byte bitmask
     * @return updated byte bitmask with invisible bit set
     */
    public byte computeDataWatcherMask(byte existingMask) {
        if (enforceInvisibility) {
            return (byte) (existingMask | METADATA_FLAG_INVISIBLE);
        }
        return existingMask;
    }

    public boolean isEnforceInvisibility() {
        return enforceInvisibility;
    }

    public void setEnforceInvisibility(boolean enforceInvisibility) {
        this.enforceInvisibility = enforceInvisibility;
    }

    public boolean isEnforceSilence() {
        return enforceSilence;
    }

    public void setEnforceSilence(boolean enforceSilence) {
        this.enforceSilence = enforceSilence;
    }
}
