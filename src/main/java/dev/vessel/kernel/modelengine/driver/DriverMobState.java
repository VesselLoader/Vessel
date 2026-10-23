package dev.vessel.kernel.modelengine.driver;

import java.util.Objects;
import java.util.UUID;

/**
 * Mutable runtime state of the native root controller (driver) entity in Model Engine.
 * Represents the invisible, silent mob driving vanilla AI, navigation, pathfinding, and physics.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DriverMobState {

    private final int entityId;
    private final UUID uniqueId;
    private final String entityType;

    private double posX;
    private double posY;
    private double posZ;

    private float yaw;
    private float pitch;

    private double motionX;
    private double motionY;
    private double motionZ;

    private float currentHealth = 20.0f;
    private float maxHealth = 20.0f;

    private boolean invisible = true;
    private boolean silent = true;
    private boolean alive = true;

    /**
     * Constructs a DriverMobState.
     *
     * @param entityId   Minecraft entity ID
     * @param uniqueId   Entity UUID
     * @param entityType namespaced entity type (e.g. "minecraft:pig", "minecraft:zombie")
     */
    public DriverMobState(int entityId, UUID uniqueId, String entityType) {
        this.entityId = entityId;
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId cannot be null");
        this.entityType = Objects.requireNonNull(entityType, "entityType cannot be null");
    }

    public int getEntityId() {
        return entityId;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getEntityType() {
        return entityType;
    }

    public double getPosX() {
        return posX;
    }

    public void setPosX(double posX) {
        this.posX = posX;
    }

    public double getPosY() {
        return posY;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    public double getPosZ() {
        return posZ;
    }

    public void setPosZ(double posZ) {
        this.posZ = posZ;
    }

    public void setPosition(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double getMotionX() {
        return motionX;
    }

    public void setMotionX(double motionX) {
        this.motionX = motionX;
    }

    public double getMotionY() {
        return motionY;
    }

    public void setMotionY(double motionY) {
        this.motionY = motionY;
    }

    public double getMotionZ() {
        return motionZ;
    }

    public void setMotionZ(double motionZ) {
        this.motionZ = motionZ;
    }

    public float getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(float currentHealth) {
        this.currentHealth = Math.max(0.0f, Math.min(this.maxHealth, currentHealth));
        if (this.currentHealth <= 0.0f) {
            this.alive = false;
        }
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(float maxHealth) {
        if (maxHealth <= 0.0f) {
            throw new IllegalArgumentException("maxHealth must be positive: " + maxHealth);
        }
        this.maxHealth = maxHealth;
        if (this.currentHealth > maxHealth) {
            this.currentHealth = maxHealth;
        }
    }

    public boolean isInvisible() {
        return invisible;
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
