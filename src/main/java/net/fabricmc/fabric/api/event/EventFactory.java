package net.fabricmc.fabric.api.event;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Canonical Fabric API factory for constructing {@link Event} instances
 * ({@code net.fabricmc.fabric.api.event.EventFactory}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class EventFactory {

    private EventFactory() {
    }

    /**
     * Creates an array-backed {@link Event} for the specified listener contract type.
     *
     * @param type           listener interface class token
     * @param invokerFactory factory producing a composite invoker over the registered listener list
     * @param <T>            listener contract type
     * @return new array-backed {@link Event}
     */
    public static <T> Event<T> createArrayBacked(
            Class<? super T> type, Function<List<T>, T> invokerFactory) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(invokerFactory, "invokerFactory must not be null");
        return new ArrayBackedFabricEvent<>(invokerFactory);
    }
}
