package dev.vessel.kernel.ir;

import java.util.Objects;

/**
 * Enumeration defining the normalized semantic bytecode hook intents supported by VESSEL IR.
 *
 * <p>Rather than executing raw ASM Mixin injections in arbitrary orders which trigger bytecode
 * collisions, VESSEL decompiles foreign mod hooks into declarative semantic intents. These intents
 * are deterministically re-ordered and woven into target classes during load time in a single pass.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-3)
 */
public enum VesselSemanticHookIntent {

    /**
     * Executes hook logic at the entry point of the target method before any original instructions.
     * Equivalent to Fabric/Forge {@code @Inject(at = @At("HEAD"))}.
     */
    METHOD_PRE_EXECUTION("head", true),

    /**
     * Executes hook logic immediately before the target method returns, capable of observing or altering return values.
     * Equivalent to Fabric/Forge {@code @Inject(at = @At("RETURN"))}.
     */
    METHOD_POST_EXECUTION("return", true),

    /**
     * Intercepts and redirects reads or writes to a specific field.
     * Equivalent to Fabric/Forge {@code @Redirect} on field accesses.
     */
    FIELD_ACCESS_INTERCEPTION("field_redirect", false),

    /**
     * Intercepts method arguments or local variables within a method invocation.
     * Equivalent to Fabric/Forge {@code @ModifyArg} or {@code @ModifyVariable}.
     */
    METHOD_INVOCATION_MUTATION("modify_arg", false),

    /**
     * Wraps an entire operation or method invocation pipeline, delegating to an {@code Operation<T>} handler.
     * Equivalent to MixinExtras {@code @WrapOperation} and {@code @WrapMethod}.
     * Essential for modern performance mod compatibility (e.g. Sodium, Iris, Create, Lithium).
     */
    METHOD_OPERATION_WRAPPING("wrap_operation", false);

    private final String id;
    private final boolean supportsCancellation;

    VesselSemanticHookIntent(String id, boolean supportsCancellation) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.supportsCancellation = supportsCancellation;
    }

    /**
     * Returns the standardized intent identifier.
     *
     * @return the intent identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Indicates whether this hook intent natively supports early method cancellation / short-circuiting.
     *
     * @return true if cancellable
     */
    public boolean supportsCancellation() {
        return supportsCancellation;
    }
}
