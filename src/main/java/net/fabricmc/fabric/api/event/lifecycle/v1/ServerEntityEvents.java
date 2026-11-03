package net.fabricmc.fabric.api.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Canonical Fabric Lifecycle Events v1 server entity load and unload event hooks
 * ({@code net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ServerEntityEvents {

    /**
     * Event fired when an entity is loaded into a server world.
     */
    public static final Event<EntityLifecycleCallback> ENTITY_LOAD = EventFactory.createArrayBacked(
            EntityLifecycleCallback.class,
            callbacks -> (entity, world) -> {
                for (EntityLifecycleCallback cb : callbacks) {
                    cb.onEntityLifecycle(entity, world);
                }
            });

    /**
     * Event fired when an entity is unloaded from a server world.
     */
    public static final Event<EntityLifecycleCallback> ENTITY_UNLOAD = EventFactory.createArrayBacked(
            EntityLifecycleCallback.class,
            callbacks -> (entity, world) -> {
                for (EntityLifecycleCallback cb : callbacks) {
                    cb.onEntityLifecycle(entity, world);
                }
            });

    private ServerEntityEvents() {
    }
}
