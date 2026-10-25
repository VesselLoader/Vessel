package dev.vessel.kernel.transform.mixin;

import java.util.Objects;

/**
 * Immutable carrier identifying an individual bytecode call site targeted by @WrapOperation.
 *
 * @param targetClass class internal name containing the call site
 * @param enclosingMethodName method enclosing the call instruction
 * @param enclosingMethodDescriptor descriptor of the enclosing method
 * @param callOpcode invocation opcode (e.g. INVOKEVIRTUAL, INVOKESTATIC)
 * @param callOwner owner of the method being invoked
 * @param callName name of the method being invoked
 * @param callDesc descriptor of the method being invoked
 * @param ordinal occurrence ordinal in the method body
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record WrapOperationTarget(
        String targetClass,
        String enclosingMethodName,
        String enclosingMethodDescriptor,
        int callOpcode,
        String callOwner,
        String callName,
        String callDesc,
        int ordinal
) {
    public WrapOperationTarget {
        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        Objects.requireNonNull(enclosingMethodName, "enclosingMethodName cannot be null");
        Objects.requireNonNull(enclosingMethodDescriptor, "enclosingMethodDescriptor cannot be null");
        Objects.requireNonNull(callOwner, "callOwner cannot be null");
        Objects.requireNonNull(callName, "callName cannot be null");
        Objects.requireNonNull(callDesc, "callDesc cannot be null");
    }
}
