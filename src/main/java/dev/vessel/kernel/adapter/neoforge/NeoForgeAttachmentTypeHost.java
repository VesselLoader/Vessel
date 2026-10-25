package dev.vessel.kernel.adapter.neoforge;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Host coordinator managing NeoForge Data Attachments across entities, chunks, and levels.
 * <p>
 * Eliminates ERR-99 (AttachmentType leak across chunk unloads and entity lifecycle drops)
 * and resolves ECO-041 through ECO-050.
 * Enforces explicit holder eviction upon world/chunk unload boundaries.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class NeoForgeAttachmentTypeHost {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeoForgeAttachmentTypeHost.class);

    public static final String ERROR_CODE_ATTACHMENT_LEAK = "ERR-99";
    public static final String ERROR_CODE_ECOSYSTEM_ATTACHMENT = "ECO-041";

    private final Map<String, NeoForgeAttachmentKey<?>> registeredTypes = new ConcurrentHashMap<>();
    private final Map<String, NeoForgeAttachmentContainer> holderContainers = new ConcurrentHashMap<>();

    /**
     * Constructs a new attachment type host.
     */
    public NeoForgeAttachmentTypeHost() {
    }

    /**
     * Registers a typed data attachment definition.
     *
     * @param key attachment key metadata
     * @param <T> data value type
     */
    public <T> void registerAttachmentType(NeoForgeAttachmentKey<T> key) {
        Objects.requireNonNull(key, "key must not be null");
        registeredTypes.put(key.namespacedKey(), key);
        LOGGER.debug("[VESSEL-ATTACHMENT] Registered attachment type: {}", key.namespacedKey());
    }

    /**
     * Retrieves an attachment for a holder. If not explicitly set, supplies default value if configured.
     *
     * @param holder target attachment holder
     * @param key    attachment type key
     * @param <T>    value type
     * @return stored or default value
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(NeoForgeAttachmentHolder holder, NeoForgeAttachmentKey<T> key) {
        Objects.requireNonNull(holder, "holder must not be null");
        Objects.requireNonNull(key, "key must not be null");

        String holderId = holder.getAttachmentHolderId();
        NeoForgeAttachmentContainer container = holderContainers.get(holderId);

        if (container != null && container.has(key.namespacedKey())) {
            Object raw = container.get(key.namespacedKey());
            if (raw != null) {
                if (!key.valueClass().isInstance(raw)) {
                    String err = String.format("Type mismatch for attachment '%s': expected %s, got %s",
                            key.namespacedKey(), key.valueClass().getName(), raw.getClass().getName());
                    LOGGER.error("[VESSEL-ATTACHMENT] [{}] {}", ERROR_CODE_ATTACHMENT_LEAK, err);
                    throw new NeoForgeAttachmentException(ERROR_CODE_ATTACHMENT_LEAK, holderId, key.namespacedKey(), err);
                }
                return (T) raw;
            }
        }

        // Fall back to default supplier if provided
        if (key.defaultSupplier() != null) {
            T def = key.defaultSupplier().get();
            if (def != null) {
                setData(holder, key, def);
            }
            return def;
        }

        return null;
    }

    /**
     * Stores an attachment value on a target holder.
     *
     * @param holder target attachment holder
     * @param key    attachment type key
     * @param value  non-null value to store
     * @param <T>    value type
     */
    public <T> void setData(NeoForgeAttachmentHolder holder, NeoForgeAttachmentKey<T> key, T value) {
        Objects.requireNonNull(holder, "holder must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");

        if (!key.valueClass().isInstance(value)) {
            String err = String.format("Cannot set attachment '%s': value %s is not an instance of %s",
                    key.namespacedKey(), value.getClass().getName(), key.valueClass().getName());
            LOGGER.error("[VESSEL-ATTACHMENT] [{}] {}", ERROR_CODE_ATTACHMENT_LEAK, err);
            throw new NeoForgeAttachmentException(
                    ERROR_CODE_ATTACHMENT_LEAK,
                    holder.getAttachmentHolderId(),
                    key.namespacedKey(),
                    err
            );
        }

        String holderId = holder.getAttachmentHolderId();
        NeoForgeAttachmentContainer container = holderContainers.computeIfAbsent(
                holderId,
                NeoForgeAttachmentContainer::new
        );
        container.set(key.namespacedKey(), value);
    }

    /**
     * Checks if a holder currently has an explicit attachment value.
     *
     * @param holder target holder
     * @param key    attachment key
     * @return true if stored
     */
    public boolean hasData(NeoForgeAttachmentHolder holder, NeoForgeAttachmentKey<?> key) {
        Objects.requireNonNull(holder, "holder must not be null");
        Objects.requireNonNull(key, "key must not be null");

        NeoForgeAttachmentContainer container = holderContainers.get(holder.getAttachmentHolderId());
        return container != null && container.has(key.namespacedKey());
    }

    /**
     * Removes an attachment value from a holder.
     *
     * @param holder target holder
     * @param key    attachment key
     * @param <T>    value type
     * @return removed value or null
     */
    @SuppressWarnings("unchecked")
    public <T> T removeData(NeoForgeAttachmentHolder holder, NeoForgeAttachmentKey<T> key) {
        Objects.requireNonNull(holder, "holder must not be null");
        Objects.requireNonNull(key, "key must not be null");

        NeoForgeAttachmentContainer container = holderContainers.get(holder.getAttachmentHolderId());
        if (container == null) {
            return null;
        }
        return (T) container.remove(key.namespacedKey());
    }

    /**
     * Evicts all attachments associated with a holder upon unload or entity death, suppressing memory leaks.
     *
     * @param holder holder to evict
     */
    public void releaseHolder(NeoForgeAttachmentHolder holder) {
        Objects.requireNonNull(holder, "holder must not be null");
        releaseHolderById(holder.getAttachmentHolderId());
    }

    /**
     * Evicts all attachments by raw holder ID string.
     *
     * @param holderId raw unique ID
     */
    public void releaseHolderById(String holderId) {
        Objects.requireNonNull(holderId, "holderId must not be null");
        NeoForgeAttachmentContainer removed = holderContainers.remove(holderId);
        if (removed != null) {
            removed.clear();
            LOGGER.debug("[VESSEL-ATTACHMENT] Evicted container for holder '{}'", holderId);
        }
    }

    /**
     * Returns the active count of tracked holders.
     *
     * @return number of tracked holders
     */
    public int getTrackedHolderCount() {
        return holderContainers.size();
    }

    /**
     * Returns an unmodifiable set of all registered attachment type keys.
     *
     * @return set of type keys
     */
    public Set<String> getRegisteredTypeKeys() {
        return Collections.unmodifiableSet(registeredTypes.keySet());
    }

    /**
     * Clears all registered types and active holder containers.
     */
    public void clearAll() {
        holderContainers.clear();
        registeredTypes.clear();
    }
}
