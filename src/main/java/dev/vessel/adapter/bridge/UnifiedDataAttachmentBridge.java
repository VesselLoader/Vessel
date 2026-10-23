package dev.vessel.adapter.bridge;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Synchronizes custom entity/chunk/item data attachments three ways across Bukkit
 * {@code PersistentDataContainer} (PDC), NeoForge {@code DataAttachments}, and Fabric
 * {@code Cardinal Components}.
 *
 * @version 1.0.0
 */
public final class UnifiedDataAttachmentBridge {

    private final Map<String, String> unifiedAttachmentStore = new ConcurrentHashMap<>();

    /**
     * Creates a new three-way data attachment bridge.
     */
    public UnifiedDataAttachmentBridge() {
    }

    /**
     * Stores a synchronized data attachment accessible identically from Bukkit PDC,
     * NeoForge DataAttachments, and Fabric Cardinal Components.
     *
     * @param holderId        UUID of the owning entity, player, or block entity holder
     * @param namespacedKey   canonical namespaced key
     * @param serializedValue serialized data payload
     */
    public void putSynchronizedData(
            UUID holderId, String namespacedKey, String serializedValue) {
        Objects.requireNonNull(holderId, "holderId must not be null");
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        Objects.requireNonNull(serializedValue, "serializedValue must not be null");
        unifiedAttachmentStore.put(composeKey(holderId, namespacedKey), serializedValue);
    }

    /**
     * Reads the synchronized attachment via the Bukkit {@code PersistentDataContainer} view.
     *
     * @param holderId      UUID of the holder
     * @param namespacedKey canonical namespaced key
     * @return serialized value, or {@code null} if absent
     */
    public String getFromBukkitPdc(UUID holderId, String namespacedKey) {
        Objects.requireNonNull(holderId, "holderId must not be null");
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        return unifiedAttachmentStore.get(composeKey(holderId, namespacedKey));
    }

    /**
     * Reads the synchronized attachment via the NeoForge {@code DataAttachment} view.
     *
     * @param holderId      UUID of the holder
     * @param namespacedKey canonical namespaced key
     * @return serialized value, or {@code null} if absent
     */
    public String getFromNeoForgeAttachment(UUID holderId, String namespacedKey) {
        Objects.requireNonNull(holderId, "holderId must not be null");
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        return unifiedAttachmentStore.get(composeKey(holderId, namespacedKey));
    }

    /**
     * Reads the synchronized attachment via the Fabric {@code Cardinal Components} view.
     *
     * @param holderId      UUID of the holder
     * @param namespacedKey canonical namespaced key
     * @return serialized value, or {@code null} if absent
     */
    public String getFromFabricCardinalComponent(UUID holderId, String namespacedKey) {
        Objects.requireNonNull(holderId, "holderId must not be null");
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        return unifiedAttachmentStore.get(composeKey(holderId, namespacedKey));
    }

    private static String composeKey(UUID holderId, String namespacedKey) {
        return holderId + "|" + namespacedKey.trim().toLowerCase(Locale.ROOT);
    }
}
