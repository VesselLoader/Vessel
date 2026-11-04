package net.minecraft.world.level.storage.loot.entries;

/**
 * Universal cross-era shim for Mojang LootPoolEntryContainer$Builder.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public abstract class LootPoolEntryContainer$Builder<T extends LootPoolEntryContainer$Builder<T>> {

    public LootPoolEntryContainer$Builder() {
    }

    @SuppressWarnings("unchecked")
    protected T getThis() {
        return (T) this;
    }
}
