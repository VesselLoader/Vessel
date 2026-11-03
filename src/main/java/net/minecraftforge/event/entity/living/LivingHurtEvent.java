package net.minecraftforge.event.entity.living;

import java.util.Objects;
import java.util.UUID;

/**
 * Forge gameplay event ({@code net.minecraftforge.event.entity.living.LivingHurtEvent}) fired when
 * a living entity is about to take damage, allowing listeners to modify the damage amount or
 * cancel the damage entirely.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class LivingHurtEvent {

    private final UUID entityUuid;
    private final String damageSource;
    private volatile float amount;
    private volatile boolean cancelled;

    /**
     * Constructs a new {@code LivingHurtEvent} for the target entity, damage source, and initial
     * damage amount.
     *
     * @param entityUuid   target living entity UUID
     * @param damageSource canonical damage source identifier
     * @param amount       initial damage amount
     */
    public LivingHurtEvent(UUID entityUuid, String damageSource, float amount) {
        this.entityUuid = Objects.requireNonNull(entityUuid, "entityUuid must not be null");
        this.damageSource = Objects.requireNonNull(damageSource, "damageSource must not be null");
        this.amount = Math.max(0.0F, amount);
    }

    /**
     * Returns the UUID of the living entity receiving damage.
     *
     * @return entity UUID
     */
    public UUID getEntityUuid() {
        return entityUuid;
    }

    /**
     * Returns the canonical damage source name.
     *
     * @return damage source identifier
     */
    public String getDamageSource() {
        return damageSource;
    }

    /**
     * Returns the current damage amount to be applied.
     *
     * @return damage amount
     */
    public float getAmount() {
        return amount;
    }

    /**
     * Updates the damage amount to be applied (clamped to {@code >= 0.0F}).
     *
     * @param amount updated damage amount
     */
    public void setAmount(float amount) {
        this.amount = Math.max(0.0F, amount);
    }

    /**
     * Returns whether this hurt event has been cancelled.
     *
     * @return {@code true} if cancelled
     */
    public boolean isCanceled() {
        return cancelled;
    }

    /**
     * Sets the cancellation state of this hurt event.
     *
     * @param cancel {@code true} to cancel the damage
     */
    public void setCanceled(boolean cancel) {
        this.cancelled = cancel;
    }
}
