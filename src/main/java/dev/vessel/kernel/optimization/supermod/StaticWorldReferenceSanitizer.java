package dev.vessel.kernel.optimization.supermod;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * Purges retained unloaded {@code ServerLevel}, {@code Level}, or {@code ServerPlayer} instances
 * from static {@link Map} caches held by poorly written mods and plugins upon dimension unload or
 * player disconnect.
 *
 * @version 1.0.0
 */
public final class StaticWorldReferenceSanitizer {

    /**
     * Removes any entry from {@code modStaticMap} whose key or value matches
     * {@code unloadedWorldOrPlayer}.
     *
     * @param modStaticMap          the mutable static map owned by a mod or plugin
     * @param unloadedWorldOrPlayer the unloaded world or disconnected player instance
     * @return the number of retained entries purged from the map
     */
    public int purgeRetainedObjectFromStaticMap(
            Map<?, ?> modStaticMap,
            Object unloadedWorldOrPlayer) {
        if (modStaticMap == null || unloadedWorldOrPlayer == null || modStaticMap.isEmpty()) {
            return 0;
        }
        int purged = 0;
        Iterator<? extends Map.Entry<?, ?>> iterator = modStaticMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            if (Objects.equals(entry.getKey(), unloadedWorldOrPlayer)
                    || Objects.equals(entry.getValue(), unloadedWorldOrPlayer)) {
                iterator.remove();
                purged++;
            }
        }
        return purged;
    }
}
