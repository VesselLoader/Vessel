package dev.vessel.kernel.transform.mixin;

import java.util.Objects;

/**
 * Immutable carrier representing a single @WrapOperation handler interceptor method.
 *
 * @param modId identifier of the contributing mod
 * @param mixinClassName internal name of the declaring mixin class
 * @param handlerName name of the handler method
 * @param handlerDescriptor method descriptor of the handler
 * @param priority execution priority of this handler (lower runs earlier)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record WrapOperationHandler(
        String modId,
        String mixinClassName,
        String handlerName,
        String handlerDescriptor,
        int priority
) {
    public WrapOperationHandler {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(mixinClassName, "mixinClassName cannot be null");
        Objects.requireNonNull(handlerName, "handlerName cannot be null");
        Objects.requireNonNull(handlerDescriptor, "handlerDescriptor cannot be null");
    }
}
