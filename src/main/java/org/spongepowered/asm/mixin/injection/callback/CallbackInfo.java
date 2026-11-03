package org.spongepowered.asm.mixin.injection.callback;

/**
 * Canonical SpongePowered Mixin callback control carrier for {@code void} target methods
 * ({@code org.spongepowered.asm.mixin.injection.callback.CallbackInfo}).
 *
 * <p>Passed to {@link org.spongepowered.asm.mixin.injection.Inject} callback methods to expose
 * the callback identifier and allow cancellable callbacks to abort early via {@link #cancel()}.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class CallbackInfo {

    private final String name;
    private final boolean cancellable;
    private boolean cancelled;

    /**
     * Constructs a {@code CallbackInfo} with explicit cancellability.
     *
     * @param name        callback identifier (typically target method name)
     * @param cancellable whether {@link #cancel()} is permitted
     */
    public CallbackInfo(String name, boolean cancellable) {
        this.name = (name == null || name.isBlank()) ? "callback" : name;
        this.cancellable = cancellable;
    }

    /**
     * Constructs a non-cancellable {@code CallbackInfo}.
     *
     * @param name callback identifier
     */
    public CallbackInfo(String name) {
        this(name, false);
    }

    /**
     * Returns the identifier of this callback.
     *
     * @return callback identifier string
     */
    public String getId() {
        return name;
    }

    /**
     * Returns whether this callback is permitted to cancel the target method execution.
     *
     * @return {@code true} if cancellable
     */
    public final boolean isCancellable() {
        return cancellable;
    }

    /**
     * Returns whether this callback has been cancelled via {@link #cancel()}.
     *
     * @return {@code true} if cancelled
     */
    public final boolean isCancelled() {
        return cancelled;
    }

    /**
     * Marks this callback as cancelled so the target method returns early.
     *
     * @throws IllegalStateException if this callback is not cancellable
     */
    public void cancel() {
        if (!cancellable) {
            throw new IllegalStateException("CallbackInfo is not cancellable: " + name);
        }
        this.cancelled = true;
    }
}
