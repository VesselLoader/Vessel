package dev.vessel.kernel.event;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Fabric and Bukkit-inspired array-backed event invoker.
 * <p>
 * Replaces dynamic reflection ({@code Method.invoke}) and synchronized collections with
 * an immutable, pre-baked flat array of listener delegates.
 * During event execution, dispatch is performed through a contiguous array index loop,
 * enabling full JIT devirtualization, loop unrolling, and sub-nanosecond dispatch speed.
 *
 * @param <T> event type
 * @version 1.0.1
 */
public final class ArrayBackedEventInvoker<T> {

    private static final Consumer<?>[] EMPTY = new Consumer<?>[0];

    @SuppressWarnings("unchecked")
    private volatile Consumer<T>[] bakedListeners = (Consumer<T>[]) EMPTY;
    private final List<Consumer<T>> listenerList = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new array-backed invoker with no initial listeners.
     */
    public ArrayBackedEventInvoker() {
        bake();
    }

    /**
     * Registers a listener delegate and re-bakes the execution array.
     *
     * @param listener consumer delegate
     */
    public synchronized void register(Consumer<T> listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listenerList.add(listener);
        bake();
    }

    /**
     * Unregisters a listener delegate and re-bakes the execution array.
     *
     * @param listener consumer delegate to remove
     * @return true if removed
     */
    public synchronized boolean unregister(Consumer<T> listener) {
        boolean removed = listenerList.remove(listener);
        if (removed) {
            bake();
        }
        return removed;
    }

    /**
     * Dispatches an event to all baked listeners in order, respecting event cancellation.
     *
     * @param event the event object
     */
    public void dispatch(T event) {
        dispatch(event, true);
    }

    /**
     * Dispatches an event to all baked listeners, optionally checking cancellation status.
     *
     * @param event             the event object
     * @param checkCancellation if true, execution breaks when the event is cancelled
     */
    public void dispatch(T event, boolean checkCancellation) {
        Consumer<T>[] listeners = this.bakedListeners;
        boolean isCancellable = event instanceof VesselCancellable;

        for (int i = 0; i < listeners.length; i++) {
            if (checkCancellation && isCancellable && ((VesselCancellable) event).isCancelled()) {
                break;
            }
            listeners[i].accept(event);
        }
    }

    /**
     * Re-bakes the internal list into a flat contiguous array.
     */
    @SuppressWarnings("unchecked")
    private void bake() {
        this.bakedListeners = listenerList.toArray((Consumer<T>[]) new Consumer<?>[0]);
    }

    /**
     * Returns the number of registered listeners.
     *
     * @return listener count
     */
    public int getListenerCount() {
        return bakedListeners.length;
    }

    /**
     * Clears all registered listeners.
     */
    public synchronized void clear() {
        listenerList.clear();
        bake();
    }
}
