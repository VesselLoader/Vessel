package dev.vessel.kernel.optimization.entity;

/**
 * Mutable spatial coordinate container for active player LOD distance evaluation.
 * <p>
 * Eliminates high-frequency heap allocations during player movements by permitting in-place
 * volatile coordinate updates without recreating instances or invalidating cache arrays.
 * </p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MutablePlayerPosition {

    private volatile double x;
    private volatile double y;
    private volatile double z;

    /**
     * Constructs a new MutablePlayerPosition.
     *
     * @param x initial coordinate X
     * @param y initial coordinate Y
     * @param z initial coordinate Z
     */
    public MutablePlayerPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Updates coordinates in-place with volatile memory visibility.
     *
     * @param x new X
     * @param y new Y
     * @param z new Z
     */
    public void update(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }
}
