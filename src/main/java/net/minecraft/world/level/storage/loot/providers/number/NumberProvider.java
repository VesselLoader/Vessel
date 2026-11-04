package net.minecraft.world.level.storage.loot.providers.number;

/**
 * Universal cross-era stub for NumberProvider, bridging Minecraft 1.20.1 loot table datagen.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public interface NumberProvider {

    default float getFloat(Object context) {
        return 1.0f;
    }

    default int getInt(Object context) {
        return 1;
    }
}
