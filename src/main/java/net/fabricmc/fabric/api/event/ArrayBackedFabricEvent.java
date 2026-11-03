package net.fabricmc.fabric.api.event;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Thread-safe array-backed implementation of Fabric's {@link Event} contract
 * ({@code net.fabricmc.fabric.api.event.ArrayBackedFabricEvent}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> functional listener contract type
 * @version 1.0.0
 */
public final class ArrayBackedFabricEvent<T> extends Event<T> {

    private final Function<List<T>, T> invokerFactory;
    private final List<T> listeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs an {@code ArrayBackedFabricEvent} using the given composite invoker factory.
     *
     * @param invokerFactory non-null factory producing a composite invoker from a listener list
     */
    public ArrayBackedFabricEvent(Function<List<T>, T> invokerFactory) {
        this.invokerFactory = Objects.requireNonNull(invokerFactory, "invokerFactory must not be null");
        this.invoker = invokerFactory.apply(Collections.unmodifiableList(listeners));
    }

    @Override
    public void register(T listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
        this.invoker = invokerFactory.apply(Collections.unmodifiableList(listeners));
    }

    @Override
    public List<T> getRegisteredListeners() {
        return Collections.unmodifiableList(listeners);
    }

    @Override
    public void clearListeners() {
        listeners.clear();
        this.invoker = invokerFactory.apply(Collections.unmodifiableList(listeners));
    }
}
