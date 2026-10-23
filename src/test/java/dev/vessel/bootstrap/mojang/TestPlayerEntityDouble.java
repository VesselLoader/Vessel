package dev.vessel.bootstrap.mojang;

/**
 * Top-level test double representing an in-game player entity for verifying multi-era Bukkit
 * command execution and coordinate handling without inner classes.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class TestPlayerEntityDouble {

    private double x = 120.5;
    private double y = 65.0;
    private double z = -300.25;
    private float yaw = 90.0f;
    private float pitch = 15.0f;
    private boolean teleported;

    public TestPlayerEntityDouble() {
    }

    public Object getPlayer() {
        return this;
    }

    public String getName() {
        return "Steve";
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYRot() {
        return yaw;
    }

    public float getXRot() {
        return pitch;
    }

    public void teleportTo(double targetX, double targetY, double targetZ) {
        this.x = targetX;
        this.y = targetY;
        this.z = targetZ;
        this.teleported = true;
    }

    public boolean isTeleported() {
        return teleported;
    }
}
