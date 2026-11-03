package net.minecraftforge.event.entity.living;

import java.util.Objects;
import java.util.UUID;
import net.minecraftforge.eventbus.api.Event;

/**
 * Forge living entity death event ({@code net.minecraftforge.event.entity.living.LivingDeathEvent})
 * fired on {@code MinecraftForge.EVENT_BUS} when a living entity takes fatal damage.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class LivingDeathEvent extends Event {

    private final UUID entityUuid;
    private final String damageSourceId;

    /**
     * Constructs a new {@code LivingDeathEvent} for the specified dying entity UUID and damage source.
     *
     * @param entityUuid     non-null dying entity UUID
     * @param damageSourceId non-null namespaced damage source identifier
     */
    public LivingDeathEvent(UUID entityUuid, String damageSourceId) {
        this.entityUuid = Objects.requireNonNull(entityUuid, "entityUuid must not be null");
        this.damageSourceId = Objects.requireNonNull(damageSourceId, "damageSourceId must not be null").trim();
    }

    /**
     * Returns the UUID of the dying entity.
     *
     * @return non-null entity UUID
     */
    public UUID getEntityUuid() {
        return entityUuid;
    }

    /**
     * Returns the namespaced damage source identifier that caused the entity's death.
     *
     * @return non-null damage source identifier
     */
    public String getDamageSourceId() {
        return damageSourceId;
    }

    /**
     * Returns {@code true} indicating that {@code LivingDeathEvent} is cancellable.
     *
     * @return {@code true}
     */
    @Override
    public boolean isCancelable() {
        return true;
    }
}
