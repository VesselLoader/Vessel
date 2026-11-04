package net.minecraftforge.event.server;

import net.minecraftforge.eventbus.api.Event;

/**
 * Universal multi-era event fired on MinecraftForge.EVENT_BUS when the server begins its shutdown sequence.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public final class ServerStoppingEvent extends Event {

    private final Object server;

    public ServerStoppingEvent(Object server) {
        this.server = server;
    }

    public ServerStoppingEvent() {
        this(null);
    }

    public Object getServer() {
        return server;
    }
}
