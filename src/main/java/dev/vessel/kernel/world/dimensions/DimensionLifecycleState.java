package dev.vessel.kernel.world.dimensions;

/**
 * State machine stages for dynamic Minecraft dimensions.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum DimensionLifecycleState {

    /**
     * Dimension has not been registered in either Bukkit or Mojang registries.
     */
    UNREGISTERED,

    /**
     * Dimension key is registered in Mojang ServerLevel registry but chunks are not yet loaded.
     */
    REGISTERED,

    /**
     * Dimension chunks, world border, and spawn region are actively initializing.
     */
    LOADING,

    /**
     * Dimension is fully active, ticking entities, and accessible to Bukkit plugins and modded players.
     */
    ACTIVE,

    /**
     * Dimension is flushing chunk dirty masks, saving player data, and draining entity actor strands.
     */
    UNLOADING,

    /**
     * Dimension has successfully saved and completely released native and JVM heap resources.
     */
    UNLOADED
}
