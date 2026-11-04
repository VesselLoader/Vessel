package net.minecraftforge.event.entity;

import net.minecraftforge.eventbus.api.Event;

/**
 * Universal multi-era base event for Forge entity lifecycle and world events.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public class EntityEvent extends Event {

    private final Object entity;

    public EntityEvent() {
        this(null);
    }

    public EntityEvent(Object entity) {
        this.entity = entity;
    }

    public Object getEntity() {
        return entity;
    }
}
