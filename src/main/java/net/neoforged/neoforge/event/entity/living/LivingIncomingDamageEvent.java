package net.neoforged.neoforge.event.entity.living;

import java.util.Objects;
import java.util.UUID;

/**
 * NeoForge gameplay event
 * ({@code net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent}) fired when a
 * living entity is about to receive incoming damage, allowing listeners to adjust the damage
 * amount or cancel the hit.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class LivingIncomingDamageEvent {

    private final UUID entityUuid;
    private final String damageSource;
    private volatile float amount;
    private volatile boolean cancelled;

    /**
     * Constructs a new {@code LivingIncomingDamageEvent} for the target entity, damage source, and
     * initial damage amount.
     *
     * @param entityUuid   target living entity UUID
     * @param damageSource canonical damage source identifier
     * @param amount       initial incoming damage amount
     */
    public LivingIncomingDamageEvent(UUID entityUuid, String damageSource, float amount) {
        this.entityUuid = Objects.requireNonNull(entityUuid, "entityUuid must not be null");
        this.damageSource = Objects.requireNonNull(damageSource, "damageSource must not be null");
        this.amount = Math.max(0.0F, amount);
    }

    /**
     * Returns the UUID of the living entity receiving incoming damage.
     *
     * @return entity UUID
     */
    public UUID getEntityUuid() {
        return entityUuid;
    }

    /**
     * Returns the canonical damage source identifier.
     *
     * @return damage source string
     */
    public String getDamageSource() {
        return damageSource;
    }

    /**
     * Returns the current incoming damage amount.
     *
     * @return damage amount
     */
    public float getAmount() {
        return amount;
    }

    /**
     * Sets the incoming damage amount (clamped to {@code >= 0.0F}).
     *
     * @param amount updated damage amount
     */
    public void setAmount(float amount) {
        this.amount = Math.max(0.0F, amount);
    }

    /**
     * Returns whether this incoming damage event has been cancelled.
     *
     * @return {@code true} if cancelled
     */
    public boolean isCanceled() {
        return cancelled;
    }

    /**
     * Sets the cancellation state of this incoming damage event.
     *
     * @param canceled {@code true} to cancel the incoming damage
     */
    public void setCanceled(boolean canceled) {
        this.cancelled = canceled;
    }
}
