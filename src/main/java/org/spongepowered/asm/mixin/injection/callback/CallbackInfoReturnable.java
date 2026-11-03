package org.spongepowered.asm.mixin.injection.callback;

/**
 * Canonical SpongePowered Mixin callback control and return-value carrier for non-{@code void}
 * target methods ({@code org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable}).
 *
 * <p>Allows cancellable {@link org.spongepowered.asm.mixin.injection.Inject} callbacks to inspect
 * or override the return value of the target method via {@link #setReturnValue(Object)}.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <R> boxed return value type
 * @version 1.0.0
 */
public final class CallbackInfoReturnable<R> extends CallbackInfo {

    private R returnValue;

    /**
     * Constructs a {@code CallbackInfoReturnable} with an initial {@code null} return value.
     *
     * @param name        callback identifier
     * @param cancellable whether {@link #setReturnValue(Object)} and {@link #cancel()} are permitted
     */
    public CallbackInfoReturnable(String name, boolean cancellable) {
        this(name, cancellable, null);
    }

    /**
     * Constructs a {@code CallbackInfoReturnable} with the specified initial return value.
     *
     * @param name        callback identifier
     * @param cancellable whether {@link #setReturnValue(Object)} and {@link #cancel()} are permitted
     * @param returnValue initial return value
     */
    public CallbackInfoReturnable(String name, boolean cancellable, R returnValue) {
        super(name, cancellable);
        this.returnValue = returnValue;
    }

    /**
     * Cancels the callback and overrides the target method's return value.
     *
     * @param returnValue replacement return value
     * @throws IllegalStateException if this callback is not cancellable
     */
    public void setReturnValue(R returnValue) {
        super.cancel();
        this.returnValue = returnValue;
    }

    /**
     * Returns the current boxed return value.
     *
     * @return current return value or {@code null}
     */
    public R getReturnValue() {
        return returnValue;
    }

    /**
     * Returns the current return value unboxed as a {@code boolean}.
     *
     * @return boolean return value, or {@code false} if not a {@link Boolean}
     */
    public boolean getReturnValueZ() {
        return returnValue instanceof Boolean b && b;
    }

    /**
     * Returns the current return value unboxed as an {@code int}.
     *
     * @return int return value, or {@code 0} if not a {@link Number}
     */
    public int getReturnValueI() {
        return returnValue instanceof Number n ? n.intValue() : 0;
    }

    /**
     * Returns the current return value unboxed as a {@code long}.
     *
     * @return long return value, or {@code 0L} if not a {@link Number}
     */
    public long getReturnValueJ() {
        return returnValue instanceof Number n ? n.longValue() : 0L;
    }

    /**
     * Returns the current return value unboxed as a {@code float}.
     *
     * @return float return value, or {@code 0.0F} if not a {@link Number}
     */
    public float getReturnValueF() {
        return returnValue instanceof Number n ? n.floatValue() : 0.0F;
    }

    /**
     * Returns the current return value unboxed as a {@code double}.
     *
     * @return double return value, or {@code 0.0D} if not a {@link Number}
     */
    public double getReturnValueD() {
        return returnValue instanceof Number n ? n.doubleValue() : 0.0D;
    }
}
