package net.minecraftforge.event;

import net.minecraftforge.eventbus.api.Event;

/**
 * Universal multi-era event fired on MinecraftForge.EVENT_BUS for mods to register resource reload listeners.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public class AddReloadListenerEvent extends Event {

    public AddReloadListenerEvent() {
    }

    public void addListener(Object listener) {
    }
}
