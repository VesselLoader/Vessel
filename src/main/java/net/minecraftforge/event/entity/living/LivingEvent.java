package net.minecraftforge.event.entity.living;

import net.minecraftforge.event.entity.EntityEvent;

/**
 * Universal multi-era base event for Forge living entity events.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public class LivingEvent extends EntityEvent {

    public LivingEvent() {
        super(null);
    }

    public LivingEvent(Object entity) {
        super(entity);
    }

    public Object getEntityLiving() {
        return getEntity();
    }
}
