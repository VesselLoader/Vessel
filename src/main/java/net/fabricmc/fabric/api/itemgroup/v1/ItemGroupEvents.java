package net.fabricmc.fabric.api.itemgroup.v1;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Canonical Fabric Item Group API v1 event hooks for modifying creative item groups
 * ({@code net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ItemGroupEvents {

    private static final Map<String, Event<ModifyEntriesCallback>> GROUP_EVENTS =
            new ConcurrentHashMap<>();

    private ItemGroupEvents() {
    }

    /**
     * Returns the {@link Event} for modifying entries of the specified creative item group.
     *
     * @param itemGroupKey non-null item group registry key
     * @return item group modification event
     */
    public static Event<ModifyEntriesCallback> modifyEntriesEvent(String itemGroupKey) {
        Objects.requireNonNull(itemGroupKey, "itemGroupKey must not be null");
        Event<ModifyEntriesCallback> existing = GROUP_EVENTS.get(itemGroupKey);
        if (existing != null) {
            return existing;
        }
        Event<ModifyEntriesCallback> created = EventFactory.createArrayBacked(
                ModifyEntriesCallback.class,
                ItemGroupEvents::createCompositeCallback);
        Event<ModifyEntriesCallback> raced = GROUP_EVENTS.putIfAbsent(itemGroupKey, created);
        return raced != null ? raced : created;
    }

    /**
     * Clears all registered item group modification events.
     */
    public static void clearAll() {
        GROUP_EVENTS.clear();
    }

    private static ModifyEntriesCallback createCompositeCallback(
            List<ModifyEntriesCallback> callbacks) {
        return entries -> {
            for (ModifyEntriesCallback cb : callbacks) {
                cb.modifyEntries(entries);
            }
        };
    }
}
