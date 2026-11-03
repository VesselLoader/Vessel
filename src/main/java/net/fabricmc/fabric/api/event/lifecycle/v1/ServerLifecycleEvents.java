package net.fabricmc.fabric.api.event.lifecycle.v1;

import java.util.List;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Canonical Fabric Lifecycle Events v1 server startup and shutdown event hooks
 * ({@code net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ServerLifecycleEvents {

    /**
     * Fired when the dedicated or integrated Minecraft server begins starting up.
     */
    public static final Event<ServerLifecycleCallback> SERVER_STARTING =
            EventFactory.createArrayBacked(
                    ServerLifecycleCallback.class,
                    ServerLifecycleEvents::createCompositeCallback);

    /**
     * Fired when the Minecraft server has finished starting and is ready to accept players.
     */
    public static final Event<ServerLifecycleCallback> SERVER_STARTED =
            EventFactory.createArrayBacked(
                    ServerLifecycleCallback.class,
                    ServerLifecycleEvents::createCompositeCallback);

    /**
     * Fired when the Minecraft server begins shutting down.
     */
    public static final Event<ServerLifecycleCallback> SERVER_STOPPING =
            EventFactory.createArrayBacked(
                    ServerLifecycleCallback.class,
                    ServerLifecycleEvents::createCompositeCallback);

    /**
     * Fired after the Minecraft server has completely stopped.
     */
    public static final Event<ServerLifecycleCallback> SERVER_STOPPED =
            EventFactory.createArrayBacked(
                    ServerLifecycleCallback.class,
                    ServerLifecycleEvents::createCompositeCallback);

    private ServerLifecycleEvents() {
    }

    private static ServerLifecycleCallback createCompositeCallback(
            List<ServerLifecycleCallback> callbacks) {
        return server -> {
            for (ServerLifecycleCallback cb : callbacks) {
                cb.onServerLifecycle(server);
            }
        };
    }
}
