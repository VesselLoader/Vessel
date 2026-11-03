package net.fabricmc.fabric.api.loot.v2;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Canonical Fabric Loot API v2 event hooks for modifying loot tables on data reload
 * ({@code net.fabricmc.fabric.api.loot.v2.LootTableEvents}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class LootTableEvents {

    /**
     * Event fired for each loaded loot table to allow dynamic pool entry injection.
     */
    public static final Event<LootTableModifyCallback> MODIFY = EventFactory.createArrayBacked(
            LootTableModifyCallback.class,
            callbacks -> (id, poolEntriesSink, source) -> {
                for (LootTableModifyCallback callback : callbacks) {
                    callback.modifyLootTable(id, poolEntriesSink, source);
                }
            });

    private LootTableEvents() {
    }
}
