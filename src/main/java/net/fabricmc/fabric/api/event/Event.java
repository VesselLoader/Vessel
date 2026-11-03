package net.fabricmc.fabric.api.event;

import java.util.List;

/**
 * Canonical Fabric API base event container ({@code net.fabricmc.fabric.api.event.Event}).
 *
 * <p>Manages a composite {@link #invoker()} and listener registration list for Fabric
 * and Quilt mods.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> functional listener contract type
 * @version 1.0.0
 */
public abstract class Event<T> {

    /**
     * Volatile composite invoker dispatching to all currently registered listeners.
     */
    protected volatile T invoker;

    /**
     * Constructs a new Fabric {@code Event}.
     */
    protected Event() {
    }

    /**
     * Returns the composite invoker instance that dispatches to all registered listeners.
     *
     * @return composite listener invoker
     */
    public final T invoker() {
        return invoker;
    }

    /**
     * Registers a new event listener and rebuilds the composite invoker.
     *
     * @param listener non-null listener instance
     */
    public abstract void register(T listener);

    /**
     * Returns an unmodifiable snapshot of all registered listeners.
     *
     * @return unmodifiable list of registered listeners
     */
    public abstract List<T> getRegisteredListeners();

    /**
     * Clears all registered listeners and resets the composite invoker.
     */
    public abstract void clearListeners();
}
