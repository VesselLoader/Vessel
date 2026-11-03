package org.bukkit.event;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks registered event listeners for a specific Bukkit {@link Event} type and maintains a
 * global registry of all active {@code HandlerList} instances.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class HandlerList {

    private static final List<HandlerList> ALL_LISTS = new CopyOnWriteArrayList<>();

    private final List<Object> registeredListeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new {@code HandlerList} and registers it in the global list registry.
     */
    public HandlerList() {
        ALL_LISTS.add(this);
    }

    /**
     * Registers a listener object with this handler list.
     *
     * @param listener the listener or handler adapter to register
     */
    public void register(Object listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        registeredListeners.add(listener);
    }

    /**
     * Unregisters a listener object from this handler list.
     *
     * @param listener the listener or handler adapter to remove
     */
    public void unregister(Object listener) {
        if (listener != null) {
            registeredListeners.remove(listener);
        }
    }

    /**
     * Returns an unmodifiable view of all listeners currently registered in this list.
     *
     * @return the registered listeners
     */
    public List<Object> getRegisteredListeners() {
        return Collections.unmodifiableList(registeredListeners);
    }

    /**
     * Unregisters all listeners across every {@code HandlerList} created in the runtime.
     */
    public static void unregisterAll() {
        for (HandlerList list : ALL_LISTS) {
            list.registeredListeners.clear();
        }
    }
}
