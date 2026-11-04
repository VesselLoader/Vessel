package net.minecraftforge.event.entity.player;

import java.util.Objects;

/**
 * Universal cross-era shim for Forge AttackEntityEvent.
 * Fired when a player attacks an entity.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class AttackEntityEvent extends PlayerEvent {

    private final Object target;

    public AttackEntityEvent() {
        this(null, null);
    }

    public AttackEntityEvent(Object player, Object target) {
        super(player);
        this.target = target;
    }

    public Object getTarget() {
        return this.target;
    }
}
