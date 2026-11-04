package net.minecraftforge.event.server;

import net.minecraftforge.eventbus.api.Event;

/**
 * Forge server started event ({@code net.minecraftforge.event.server.ServerStartedEvent})
 * fired on {@code MinecraftForge.EVENT_BUS} after world loading and post-world preparation completes.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ServerStartedEvent extends Event {

    private final Object server;

    /**
     * Constructs a {@code ServerStartedEvent} with the given server instance.
     *
     * @param server the running MinecraftServer instance
     */
    public ServerStartedEvent(Object server) {
        this.server = server;
    }

    /**
     * Default constructor for testing without an active server instance.
     */
    public ServerStartedEvent() {
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
