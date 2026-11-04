package net.minecraft.world.level;

/**
 * Universal cross-era shim for Mojang BlockAndTintGetter interface removed in Minecraft 26.3.
 * Allows legacy Forge 1.20.1 blocks and registries (such as Create AllBlocks) to initialize smoothly.
 * Maintained under VESSEL Rule 17: Standalone single-purpose interface.
 *
 * @version 1.0.0
 */
public interface BlockAndTintGetter {

    /**
     * Returns shade value for lighting calculations.
     *
     * @param direction block face
     * @param shade     shading flag
     * @return lighting shade multiplier
     */
    default float getShade(Object direction, boolean shade) {
        return 1.0F;
    }

    /**
     * Resolves block tint color.
     *
     * @param blockPos      block coordinate position
     * @param colorResolver tint color resolver
     * @return RGB tint color integer
     */
    default int getBlockTint(Object blockPos, Object colorResolver) {
        return -1;
    }
}
