package dev.vessel.kernel.transform.mixin;

import java.util.List;
import java.util.Objects;

/**
 * Immutable carrier representing a coordinated sequence of @WrapOperation handlers for a single call site.
 *
 * @param target target call site being wrapped
 * @param handlers ordered list of wrap handlers from outermost to innermost
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record WrapOperationChain(
        WrapOperationTarget target,
        List<WrapOperationHandler> handlers
) {
    public WrapOperationChain {
        Objects.requireNonNull(target, "target cannot be null");
        handlers = handlers == null ? List.of() : List.copyOf(handlers);
    }

    public int size() {
        return handlers.size();
    }

    public boolean isEmpty() {
        return handlers.isEmpty();
    }
}
