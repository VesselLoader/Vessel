package net.minecraft.client.color.block;

/**
 * Universal headless stub for Mojang BlockColor client interface.
 * Allows mod registries registering block tint colors to initialize on dedicated servers.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
@FunctionalInterface
public interface BlockColor {

    /**
     * Computes the tint color for the specified block state in the world.
     *
     * @param state     block state
     * @param level     level/world
     * @param pos       block position
     * @param tintIndex tint index layer
     * @return RGB color integer
     */
    int getColor(Object state, Object level, Object pos, int tintIndex);
}
