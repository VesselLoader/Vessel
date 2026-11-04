package net.minecraft.world.level.storage.loot.entries;

/**
 * Universal cross-era shim for Mojang LootPoolSingletonContainer$Builder.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public abstract class LootPoolSingletonContainer$Builder<T extends LootPoolSingletonContainer$Builder<T>> extends LootPoolEntryContainer$Builder<T> {

    public LootPoolSingletonContainer$Builder() {
        super();
    }
}
