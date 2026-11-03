package net.fabricmc.fabric.api.event.lifecycle.v1;

import java.util.List;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Canonical Fabric Lifecycle Events v1 server tick start/end event hooks
 * ({@code net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ServerTickEvents {

    /**
     * Fired at the start of every server tick.
     */
    public static final Event<ServerLifecycleCallback> START_SERVER_TICK =
            EventFactory.createArrayBacked(
                    ServerLifecycleCallback.class,
                    ServerTickEvents::createCompositeTickCallback);

    /**
     * Fired at the end of every server tick.
     */
    public static final Event<ServerLifecycleCallback> END_SERVER_TICK =
            EventFactory.createArrayBacked(
                    ServerLifecycleCallback.class,
                    ServerTickEvents::createCompositeTickCallback);

    private ServerTickEvents() {
    }

    private static ServerLifecycleCallback createCompositeTickCallback(
            List<ServerLifecycleCallback> callbacks) {
        return server -> {
            for (ServerLifecycleCallback cb : callbacks) {
                cb.onServerLifecycle(server);
            }
        };
    }
}
