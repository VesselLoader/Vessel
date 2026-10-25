package dev.vessel.kernel.adapter.neoforge;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe container storing key-value data attachments mapped to a specific holder.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class NeoForgeAttachmentContainer {

    private final String holderId;
    private final Map<String, Object> values = new ConcurrentHashMap<>();

    /**
     * Constructs an attachment container for a specific holder ID.
     *
     * @param holderId unique holder identifier
     */
    public NeoForgeAttachmentContainer(String holderId) {
        this.holderId = Objects.requireNonNull(holderId, "holderId must not be null");
    }

    /**
     * Returns the holder ID.
     *
     * @return holder ID
     */
    public String getHolderId() {
        return holderId;
    }

    /**
     * Retrieves an attachment value by its key.
     *
     * @param namespacedKey attachment key string
     * @return stored value or null if absent
     */
    public Object get(String namespacedKey) {
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        return values.get(namespacedKey);
    }

    /**
     * Stores an attachment value.
     *
     * @param namespacedKey attachment key string
     * @param value         value object to store
     * @return previous value or null
     */
    public Object set(String namespacedKey, Object value) {
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        Objects.requireNonNull(value, "value must not be null");
        return values.put(namespacedKey, value);
    }

    /**
     * Checks if an attachment exists.
     *
     * @param namespacedKey attachment key string
     * @return true if present
     */
    public boolean has(String namespacedKey) {
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        return values.containsKey(namespacedKey);
    }

    /**
     * Removes an attachment.
     *
     * @param namespacedKey attachment key string
     * @return removed value or null
     */
    public Object remove(String namespacedKey) {
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        return values.remove(namespacedKey);
    }

    /**
     * Returns an unmodifiable snapshot view of all stored attachments.
     *
     * @return map of key-values
     */
    public Map<String, Object> getAll() {
        return Collections.unmodifiableMap(values);
    }

    /**
     * Clears all attachments in this container.
     */
    public void clear() {
        values.clear();
    }
}
