package dev.vessel.kernel.ir;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pre-flight verification engine validating {@link VesselModuleIR} integrity before server runtime.
 * <p>
 * Performs ahead-of-time dependency graph resolution and hook target existence validation,
 * preventing runtime crashes (such as {@link NoSuchMethodError} or {@link ClassNotFoundException})
 * before world ticks commence.
 * </p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-7)
 */
public class VesselModuleIRValidator {

    /**
     * Validates that all declared dependencies of a module exist within the registered environment.
     *
     * @param module module IR to validate
     * @param availableModuleIds set of available module namespace IDs
     * @throws IllegalStateException if a required dependency is missing
     */
    public void validateDependencies(VesselModuleIR module, Set<String> availableModuleIds) {
        Objects.requireNonNull(module, "module cannot be null");
        Objects.requireNonNull(availableModuleIds, "availableModuleIds cannot be null");

        for (String req : module.requiredDependencies()) {
            if (!availableModuleIds.contains(req.toLowerCase())) {
                throw new IllegalStateException(String.format(
                        "Pre-flight verification failed for [%s]: Missing required dependency '%s'",
                        module.identifier(), req
                ));
            }
        }
    }

    /**
     * Validates that semantic hook interception targets exist on target classes.
     *
     * @param moduleIR canonical module IR containing hooks
     * @param targetClasses map of available target ClassNodes (e.g. Minecraft server classes)
     * @throws IllegalStateException if a target class or target method is missing
     */
    public void validateHookTargets(CanonicalVesselModuleIR moduleIR, Map<String, ClassNode> targetClasses) {
        Objects.requireNonNull(moduleIR, "moduleIR cannot be null");
        Objects.requireNonNull(targetClasses, "targetClasses cannot be null");

        for (VesselSemanticHookRecord hook : moduleIR.getHooks()) {
            ClassNode targetNode = targetClasses.get(hook.targetOwner());
            if (targetNode == null) {
                throw new IllegalStateException(String.format(
                        "Pre-flight verification failed: Hook [%s.%s] targets non-existent class '%s'",
                        hook.sourceClass(), hook.sourceMethod(), hook.targetOwner()
                ));
            }

            boolean methodFound = false;
            if (targetNode.methods != null) {
                for (MethodNode m : targetNode.methods) {
                    if (m.name.equals(hook.targetName()) && m.desc.equals(hook.targetDesc())) {
                        methodFound = true;
                        break;
                    }
                }
            }

            if (!methodFound) {
                throw new IllegalStateException(String.format(
                        "Pre-flight verification failed: Hook [%s.%s] targets non-existent method '%s%s' on '%s'",
                        hook.sourceClass(), hook.sourceMethod(), hook.targetName(), hook.targetDesc(), hook.targetOwner()
                ));
            }
        }
    }
}
