package org.bukkit.persistence;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;

/**
 * Thread-safe key-value storage container for custom plugin metadata attached to items, entities,
 * or tile states ({@code org.bukkit.persistence.PersistentDataContainer}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PersistentDataContainer {

    private final Map<NamespacedKey, Object> entries = new ConcurrentHashMap<>();

    /**
     * Constructs an empty {@code PersistentDataContainer}.
     */
    public PersistentDataContainer() {
    }

    /**
     * Stores a typed value under the specified {@link NamespacedKey}.
     *
     * @param key   non-null namespaced key
     * @param type  non-null persistent data type descriptor
     * @param value non-null value matching {@code type.getComplexType()}
     * @param <P>   primitive type
     * @param <C>   complex value type
     */
    public <P, C> void set(NamespacedKey key, PersistentDataType<P, C> type, C value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (!type.getComplexType().isInstance(value)) {
            throw new IllegalArgumentException(
                    "Value of type " + value.getClass().getName()
                            + " does not match expected " + type.getComplexType().getName());
        }
        if (value instanceof byte[] bytes) {
            entries.put(key, bytes.clone());
        } else {
            entries.put(key, value);
        }
    }

    /**
     * Checks whether an entry with the given {@link NamespacedKey} and matching
     * {@link PersistentDataType} exists.
     *
     * @param key  namespaced key
     * @param type persistent data type descriptor
     * @param <P>  primitive type
     * @param <C>  complex value type
     * @return {@code true} if present and of matching type
     */
    public <P, C> boolean has(NamespacedKey key, PersistentDataType<P, C> type) {
        if (key == null || type == null) {
            return false;
        }
        Object raw = entries.get(key);
        return raw != null && type.getComplexType().isInstance(raw);
    }

    /**
     * Checks whether any entry is stored under the specified {@link NamespacedKey}.
     *
     * @param key namespaced key
     * @return {@code true} if present
     */
    public boolean has(NamespacedKey key) {
        return key != null && entries.containsKey(key);
    }

    /**
     * Retrieves the typed value stored under the specified {@link NamespacedKey}, or {@code null}
     * if absent or of a different type.
     *
     * @param key  namespaced key
     * @param type persistent data type descriptor
     * @param <P>  primitive type
     * @param <C>  complex value type
     * @return stored value or {@code null}
     */
    @SuppressWarnings("unchecked")
    public <P, C> C get(NamespacedKey key, PersistentDataType<P, C> type) {
        if (key == null || type == null) {
            return null;
        }
        Object raw = entries.get(key);
        if (raw == null || !type.getComplexType().isInstance(raw)) {
            return null;
        }
        if (raw instanceof byte[] bytes) {
            return (C) bytes.clone();
        }
        return (C) raw;
    }

    /**
     * Retrieves the typed value stored under the specified {@link NamespacedKey}, or returns
     * {@code defaultValue} if absent.
     *
     * @param key          namespaced key
     * @param type         persistent data type descriptor
     * @param defaultValue fallback value
     * @param <P>          primitive type
     * @param <C>          complex value type
     * @return stored value or {@code defaultValue}
     */
    public <P, C> C getOrDefault(NamespacedKey key, PersistentDataType<P, C> type, C defaultValue) {
        C value = get(key, type);
        return value != null ? value : defaultValue;
    }

    /**
     * Removes any value stored under the specified {@link NamespacedKey}.
     *
     * @param key namespaced key to remove
     */
    public void remove(NamespacedKey key) {
        if (key != null) {
            entries.remove(key);
        }
    }

    /**
     * Checks whether this container holds no entries.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Returns an unmodifiable view of all {@link NamespacedKey}s stored in this container.
     *
     * @return set of stored keys
     */
    public Set<NamespacedKey> getKeys() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    /**
     * Creates a deep copy of this {@code PersistentDataContainer}.
     *
     * @return copied {@code PersistentDataContainer}
     */
    public PersistentDataContainer copy() {
        PersistentDataContainer clone = new PersistentDataContainer();
        for (Map.Entry<NamespacedKey, Object> entry : this.entries.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof byte[] bytes) {
                clone.entries.put(entry.getKey(), bytes.clone());
            } else {
                clone.entries.put(entry.getKey(), val);
            }
        }
        return clone;
    }
}
