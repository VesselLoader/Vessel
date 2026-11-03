package com.llamalad7.mixinextras.injector.wrapoperation;

/**
 * Functional continuation interface ({@code com.llamalad7.mixinextras.injector.wrapoperation.Operation})
 * passed to {@link WrapOperation} handlers to invoke the next wrapped operation in the chain.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <R> operation return type
 * @version 1.0.0
 */
@FunctionalInterface
public interface Operation<R> {

    /**
     * Invokes the underlying or next-in-chain operation with the given arguments.
     *
     * @param args operation arguments (including receiver instance for virtual calls)
     * @return operation result
     */
    R call(Object... args);
}
