package dev.vessel.kernel.optimization.world;

/**
 * State record tracking ephemeral light source position and last update tick.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DynamicLightSourceState {

    private long lastPos;
    private long lastUpdateTick;

    public DynamicLightSourceState(long lastPos, long lastUpdateTick) {
        this.lastPos = lastPos;
        this.lastUpdateTick = lastUpdateTick;
    }

    public synchronized long getLastPos() {
        return lastPos;
    }

    public synchronized void setLastPos(long lastPos) {
        this.lastPos = lastPos;
    }

    public synchronized long getLastUpdateTick() {
        return lastUpdateTick;
    }

    public synchronized void setLastUpdateTick(long lastUpdateTick) {
        this.lastUpdateTick = lastUpdateTick;
    }
}
