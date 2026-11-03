package net.fabricmc.loader.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Canonical Fabric/Quilt inter-mod object sharing registry
 * ({@code net.fabricmc.loader.api.ObjectShare}).
 *
 * <p>Allows mods to publish and consume shared key-value objects synchronously or
 * asynchronously via {@link #whenAvailable(String, BiConsumer)} callbacks without
 * direct compile-time coupling.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ObjectShare {

    private final Map<String, Object> entries = new ConcurrentHashMap<>();
    private final Map<String, List<BiConsumer<String, Object>>> pendingConsumers = new ConcurrentHashMap<>();

    /**
     * Constructs a new thread-safe {@code ObjectShare} registry.
     */
    public ObjectShare() {
    }

    /**
     * Retrieves the shared value associated with the given key, or {@code null} if absent.
     *
     * @param key non-null key
     * @return associated value or {@code null}
     */
    public Object get(String key) {
        Objects.requireNonNull(key, "key must not be null");
        return entries.get(key);
    }

    /**
     * Associates the specified value with the specified key and notifies any pending
     * {@link #whenAvailable(String, BiConsumer)} listeners registered for that key.
     *
     * @param key   non-null key
     * @param value non-null value
     * @return previous value associated with {@code key}, or {@code null} if none
     */
    public Object put(String key, Object value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Object previous = entries.put(key, value);
        notifyPendingConsumers(key, value);
        return previous;
    }

    /**
     * Associates the specified value with the specified key if not already present,
     * notifying any pending {@link #whenAvailable(String, BiConsumer)} listeners when inserted.
     *
     * @param key   non-null key
     * @param value non-null value
     * @return existing value if present, or {@code null} if the new value was stored
     */
    public Object putIfAbsent(String key, Object value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Object existing = entries.putIfAbsent(key, value);
        if (existing == null) {
            notifyPendingConsumers(key, value);
        }
        return existing;
    }

    /**
     * Removes the entry for the specified key if present.
     *
     * @param key non-null key
     * @return removed value or {@code null}
     */
    public Object remove(String key) {
        Objects.requireNonNull(key, "key must not be null");
        return entries.remove(key);
    }

    /**
     * Registers a callback to be invoked immediately if {@code key} is already present,
     * or deferred until a value is published for {@code key}.
     *
     * @param key      non-null key to watch
     * @param consumer non-null callback receiving {@code (key, value)}
     */
    public void whenAvailable(String key, BiConsumer<String, Object> consumer) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        Object current = entries.get(key);
        if (current != null) {
            consumer.accept(key, current);
            return;
        }
        List<BiConsumer<String, Object>> list = pendingConsumers.get(key);
        if (list == null) {
            List<BiConsumer<String, Object>> created = new CopyOnWriteArrayList<>();
            List<BiConsumer<String, Object>> raced = pendingConsumers.putIfAbsent(key, created);
            list = raced != null ? raced : created;
        }
        list.add(consumer);
        Object lateValue = entries.get(key);
        if (lateValue != null && list.remove(consumer)) {
            consumer.accept(key, lateValue);
        }
    }

    private void notifyPendingConsumers(String key, Object value) {
        List<BiConsumer<String, Object>> consumers = pendingConsumers.remove(key);
        if (consumers != null) {
            for (BiConsumer<String, Object> consumer : consumers) {
                consumer.accept(key, value);
            }
        }
    }
}
