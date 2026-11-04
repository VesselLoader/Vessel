package net.minecraftforge.event;

import net.minecraftforge.eventbus.api.Event;

/**
 * Universal multi-era event fired on MinecraftForge.EVENT_BUS when tags are synchronized or updated.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public class TagsUpdatedEvent extends Event {

    public TagsUpdatedEvent() {
    }
}
