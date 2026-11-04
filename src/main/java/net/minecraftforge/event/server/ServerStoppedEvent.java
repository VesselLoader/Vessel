package net.minecraftforge.event.server;

import net.minecraftforge.eventbus.api.Event;

/**
 * Universal multi-era event fired on MinecraftForge.EVENT_BUS when the server has completely stopped.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public final class ServerStoppedEvent extends Event {

    private final Object server;

    public ServerStoppedEvent(Object server) {
        this.server = server;
    }

    public ServerStoppedEvent() {
        this(null);
    }

    public Object getServer() {
        return server;
    }
}
