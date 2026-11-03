package dev.vessel.kernel.mixin.weaver;

import java.util.List;
import java.util.Objects;

/**
 * Immutable summary of a Mixin class inspected via reflection or ASM bytecode analysis, capturing
 * its target classes, priority, {@code @Shadow} fields, {@code @Accessor} methods,
 * {@code @Invoker} methods, and {@code @Inject} / MixinExtras injector callbacks.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class InspectedMixinSummary {

    private final String mixinInternalName;
    private final List<String> targetClassNames;
    private final int priority;
    private final List<String> shadowFieldNames;
    private final List<String> accessorMethodNames;
    private final List<String> invokerMethodNames;
    private final List<String> injectMethodNames;

    /**
     * Constructs an immutable {@code InspectedMixinSummary}.
     *
     * @param mixinInternalName   internal or binary name of the mixin class
     * @param targetClassNames    target class binary names declared on {@code @Mixin}
     * @param priority            relative mixin application priority (default {@code 1000})
     * @param shadowFieldNames    field names annotated with {@code @Shadow}
     * @param accessorMethodNames method names annotated with {@code @Accessor}
     * @param invokerMethodNames  method names annotated with {@code @Invoker}
     * @param injectMethodNames   method names annotated with {@code @Inject} or MixinExtras injectors
     */
    public InspectedMixinSummary(
            String mixinInternalName,
            List<String> targetClassNames,
            int priority,
            List<String> shadowFieldNames,
            List<String> accessorMethodNames,
            List<String> invokerMethodNames,
            List<String> injectMethodNames) {
        this.mixinInternalName = Objects.requireNonNull(mixinInternalName, "mixinInternalName must not be null");
        this.targetClassNames = List.copyOf(Objects.requireNonNull(targetClassNames, "targetClassNames must not be null"));
        this.priority = priority;
        this.shadowFieldNames = List.copyOf(Objects.requireNonNull(shadowFieldNames, "shadowFieldNames must not be null"));
        this.accessorMethodNames = List.copyOf(Objects.requireNonNull(accessorMethodNames, "accessorMethodNames must not be null"));
        this.invokerMethodNames = List.copyOf(Objects.requireNonNull(invokerMethodNames, "invokerMethodNames must not be null"));
        this.injectMethodNames = List.copyOf(Objects.requireNonNull(injectMethodNames, "injectMethodNames must not be null"));
    }

    public String getMixinInternalName() {
        return mixinInternalName;
    }

    public List<String> getTargetClassNames() {
        return targetClassNames;
    }

    public int getPriority() {
        return priority;
    }

    public List<String> getShadowFieldNames() {
        return shadowFieldNames;
    }

    public List<String> getAccessorMethodNames() {
        return accessorMethodNames;
    }

    public List<String> getInvokerMethodNames() {
        return invokerMethodNames;
    }

    public List<String> getInjectMethodNames() {
        return injectMethodNames;
    }

    /**
     * Returns {@code true} if the inspected class declares at least one {@code @Mixin} target.
     *
     * @return {@code true} if this class is an active Mixin
     */
    public boolean isMixinClass() {
        return !targetClassNames.isEmpty();
    }
}
