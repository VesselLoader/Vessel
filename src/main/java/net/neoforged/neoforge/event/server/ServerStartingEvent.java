package net.neoforged.neoforge.event.server;

import net.neoforged.bus.api.Event;

/**
 * NeoForge server starting event ({@code net.neoforged.neoforge.event.server.ServerStartingEvent})
 * fired on {@code NeoForge.EVENT_BUS} when the dedicated server has initialized network
 * sockets and command dispatchers but before world loading completes.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ServerStartingEvent extends Event {

    private final Object server;

    /**
     * Constructs a {@code ServerStartingEvent} with the given server instance.
     *
     * @param server the running MinecraftServer instance
     */
    public ServerStartingEvent(Object server) {
        this.server = server;
    }

    /**
     * Default constructor for testing without an active server instance.
     */
    public ServerStartingEvent() {
        this(null);
    }

    /**
     * Returns the active running server instance.
     *
     * @return MinecraftServer instance, or {@code null}
     */
    public Object getServer() {
        return server;
    }
}
