package net.fabricmc.fabric.api.loot.v2;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Canonical Fabric Loot API v2 callback for modifying loot table pools during reload
 * ({@code net.fabricmc.fabric.api.loot.v2.LootTableModifyCallback}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@FunctionalInterface
public interface LootTableModifyCallback {

    /**
     * Invoked when a loot table is loaded so listeners can append or modify pool entries.
     *
     * @param id              loot table identifier
     * @param poolEntriesSink mutable list of loot pool entry descriptors
     * @param source          origin of the loot table
     */
    void modifyLootTable(ResourceLocation id, List<String> poolEntriesSink, LootTableSource source);
}
