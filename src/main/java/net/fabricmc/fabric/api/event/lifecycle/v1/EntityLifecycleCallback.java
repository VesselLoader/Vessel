package net.fabricmc.fabric.api.event.lifecycle.v1;

import org.bukkit.World;
import org.bukkit.entity.Entity;

/**
 * Canonical Fabric Lifecycle Events v1 callback for entity load and unload transitions
 * ({@code net.fabricmc.fabric.api.event.lifecycle.v1.EntityLifecycleCallback}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@FunctionalInterface
public interface EntityLifecycleCallback {

    /**
     * Invoked when an entity is loaded into or unloaded from a server world.
     *
     * @param entity the affected entity
     * @param world  the world containing the entity
     */
    void onEntityLifecycle(Entity entity, World world);
}
