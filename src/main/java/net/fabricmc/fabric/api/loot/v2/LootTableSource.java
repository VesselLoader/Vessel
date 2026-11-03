package net.fabricmc.fabric.api.loot.v2;

/**
 * Canonical Fabric Loot API v2 loot table origin classification
 * ({@code net.fabricmc.fabric.api.loot.v2.LootTableSource}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum LootTableSource {
    /**
     * Built-in vanilla Minecraft loot table.
     */
    VANILLA(true),

    /**
     * Built-in mod-supplied loot table.
     */
    MOD(true),

    /**
     * External user or server data pack loot table.
     */
    DATA_PACK(false),

    /**
     * Loot table replaced programmatically during reload.
     */
    REPLACED(false);

    private final boolean builtin;

    LootTableSource(boolean builtin) {
        this.builtin = builtin;
    }

    /**
     * Returns whether this loot table originates from a built-in vanilla or mod resource.
     *
     * @return {@code true} if built-in, {@code false} if supplied by a data pack or replacement
     */
    public boolean isBuiltin() {
        return builtin;
    }
}
