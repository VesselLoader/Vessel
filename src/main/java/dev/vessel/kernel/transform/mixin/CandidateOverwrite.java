package dev.vessel.kernel.transform.mixin;

import org.objectweb.asm.tree.MethodNode;

import java.util.Objects;

/**
 * Immutable candidate representation of an @Overwrite mixin method awaiting conflict arbitration.
 *
 * @param modId identifier of the contributing mod
 * @param mixinClassName internal name of the mixin class
 * @param targetMethodName name of the method being overwritten
 * @param targetMethodDescriptor descriptor of the method being overwritten
 * @param priority declared priority of the mixin (default 1000)
 * @param handlerMethod raw ASM MethodNode containing the overwrite instructions
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record CandidateOverwrite(
        String modId,
        String mixinClassName,
        String targetMethodName,
        String targetMethodDescriptor,
        int priority,
        MethodNode handlerMethod
) {
    public CandidateOverwrite {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(mixinClassName, "mixinClassName cannot be null");
        Objects.requireNonNull(targetMethodName, "targetMethodName cannot be null");
        Objects.requireNonNull(targetMethodDescriptor, "targetMethodDescriptor cannot be null");
        Objects.requireNonNull(handlerMethod, "handlerMethod cannot be null");
    }
}
