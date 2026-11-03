package net.minecraftforge.common.util;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Forge capability holder ({@code net.minecraftforge.common.util.LazyOptional}) providing
 * lazy evaluation, caching, and invalidation callbacks for capability instances.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> held capability value type
 * @version 1.0.0
 */
public final class LazyOptional<T> {

    private static final LazyOptional<?> EMPTY = new LazyOptional<>(null);

    private final NonNullSupplier<T> supplier;
    private volatile T resolvedValue;
    private volatile boolean valid = true;
    private final List<NonNullConsumer<LazyOptional<T>>> invalidationListeners = new CopyOnWriteArrayList<>();

    private LazyOptional(NonNullSupplier<T> supplier) {
        this.supplier = supplier;
        if (supplier == null) {
            this.valid = false;
        }
    }

    /**
     * Returns the shared empty {@code LazyOptional} singleton.
     *
     * @param <T> target value type
     * @return empty {@code LazyOptional}
     */
    @SuppressWarnings("unchecked")
    public static <T> LazyOptional<T> empty() {
        return (LazyOptional<T>) EMPTY;
    }

    /**
     * Creates a new {@code LazyOptional} backed by the given lazy supplier, or returns
     * {@link #empty()} if {@code supplier} is {@code null}.
     *
     * @param supplier lazy value supplier
     * @param <T>      held value type
     * @return a new {@code LazyOptional} or {@link #empty()}
     */
    public static <T> LazyOptional<T> of(NonNullSupplier<T> supplier) {
        return supplier == null ? empty() : new LazyOptional<>(supplier);
    }

    /**
     * Returns {@code true} if this {@code LazyOptional} is valid and backed by a non-null supplier.
     *
     * @return {@code true} if present and not invalidated
     */
    public boolean isPresent() {
        return valid && supplier != null;
    }

    private T resolve() {
        if (!valid || supplier == null) {
            return null;
        }
        T current = resolvedValue;
        if (current == null) {
            current = Objects.requireNonNull(
                    supplier.get(),
                    "LazyOptional supplier must not return null");
            resolvedValue = current;
        }
        return current;
    }

    /**
     * Invokes the given consumer with the resolved value if {@link #isPresent()} is {@code true}.
     *
     * @param consumer non-null callback consumer
     */
    public void ifPresent(NonNullConsumer<? super T> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        if (isPresent()) {
            T val = resolve();
            if (val != null) {
                consumer.accept(val);
            }
        }
    }

    /**
     * Returns the resolved value if present, or {@code other} if empty or invalidated.
     *
     * @param other fallback value
     * @return resolved value or {@code other}
     */
    public T orElse(T other) {
        return isPresent() ? resolve() : other;
    }

    /**
     * Returns the resolved value if present, or evaluates {@code other} if empty or invalidated.
     *
     * @param other non-null fallback supplier
     * @return resolved value or supplier result
     */
    public T orElseGet(NonNullSupplier<? extends T> other) {
        Objects.requireNonNull(other, "other must not be null");
        return isPresent() ? resolve() : other.get();
    }

    /**
     * Returns the resolved value if present, or throws the exception created by {@code exceptionSupplier}.
     *
     * @param exceptionSupplier non-null exception supplier
     * @param <X>               exception type
     * @return resolved value
     * @throws X if empty or invalidated
     */
    public <X extends Throwable> T orElseThrow(NonNullSupplier<? extends X> exceptionSupplier) throws X {
        Objects.requireNonNull(exceptionSupplier, "exceptionSupplier must not be null");
        if (isPresent()) {
            return resolve();
        }
        throw exceptionSupplier.get();
    }

    /**
     * Registers a listener notified when this {@code LazyOptional} is invalidated.
     * If already invalid, the listener is invoked immediately.
     *
     * @param listener non-null invalidation callback
     */
    public void addListener(NonNullConsumer<LazyOptional<T>> listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        if (!valid) {
            listener.accept(this);
        } else {
            invalidationListeners.add(listener);
        }
    }

    /**
     * Invalidates this {@code LazyOptional}, clearing any cached instance and notifying all registered listeners.
     */
    public void invalidate() {
        if (!valid) {
            return;
        }
        this.valid = false;
        this.resolvedValue = null;
        for (NonNullConsumer<LazyOptional<T>> listener : invalidationListeners) {
            listener.accept(this);
        }
        invalidationListeners.clear();
    }

    /**
     * Performs an unchecked cast of this {@code LazyOptional} to the target type parameter.
     *
     * @param <X> target capability type
     * @return this instance cast to {@code LazyOptional<X>}
     */
    @SuppressWarnings("unchecked")
    public <X> LazyOptional<X> cast() {
        return (LazyOptional<X>) this;
    }
}
