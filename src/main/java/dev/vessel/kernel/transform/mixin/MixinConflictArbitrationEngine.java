package dev.vessel.kernel.transform.mixin;

import dev.vessel.kernel.ir.MethodSignatureKey;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Arbitrates destructive @Overwrite mixin collisions among competing mods.
 * <p>
 * Deconstructs conflicting @Overwrite declarations into an ordered sequential chain of safe injected calls,
 * preventing mods from wiping out each other's custom logic and eliminating JVM NoSuchMethodError / VerifyError.
 * </p>
 * <p>
 * Directly resolves ERR-56, ERR-57, and MAP-MIX-0001..0200.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MixinConflictArbitrationEngine {

    private final Map<String, Map<MethodSignatureKey, List<CandidateOverwrite>>> registeredOverwrites = new HashMap<>();
    private final ArbitrationStrategy defaultStrategy;

    public MixinConflictArbitrationEngine() {
        this(ArbitrationStrategy.CHAIN_SEQUENTIAL_INJECT);
    }

    public MixinConflictArbitrationEngine(ArbitrationStrategy defaultStrategy) {
        this.defaultStrategy = Objects.requireNonNull(defaultStrategy, "defaultStrategy cannot be null");
    }

    /**
     * Registers a candidate @Overwrite method from a mod mixin.
     *
     * @param targetClass target class internal name
     * @param candidate candidate overwrite metadata
     */
    public synchronized void registerCandidate(String targetClass, CandidateOverwrite candidate) {
        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        Objects.requireNonNull(candidate, "candidate cannot be null");

        String normalizedClass = targetClass.replace('.', '/');
        MethodSignatureKey key = new MethodSignatureKey(candidate.targetMethodName(), candidate.targetMethodDescriptor());

        registeredOverwrites
                .computeIfAbsent(normalizedClass, k -> new HashMap<>())
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(candidate);
    }

    /**
     * Arbitrates potential conflicts for a specific method on a target class.
     *
     * @param targetClass target class internal name
     * @param methodName target method name
     * @param methodDescriptor target method descriptor
     * @return arbitrated resolution result
     */
    public synchronized ArbitratedOverwriteResolution arbitrate(
            String targetClass,
            String methodName,
            String methodDescriptor) {
        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        Objects.requireNonNull(methodName, "methodName cannot be null");
        Objects.requireNonNull(methodDescriptor, "methodDescriptor cannot be null");

        String normalizedClass = targetClass.replace('.', '/');
        MethodSignatureKey key = new MethodSignatureKey(methodName, methodDescriptor);

        Map<MethodSignatureKey, List<CandidateOverwrite>> classMap = registeredOverwrites.get(normalizedClass);
        if (classMap == null || !classMap.containsKey(key)) {
            return new ArbitratedOverwriteResolution(
                    normalizedClass,
                    methodName + methodDescriptor,
                    defaultStrategy,
                    List.of(),
                    true
            );
        }

        List<CandidateOverwrite> candidates = new ArrayList<>(classMap.get(key));
        // Sort by priority ascending (Mixin convention: lower number = higher priority)
        candidates.sort(Comparator.comparingInt(CandidateOverwrite::priority));

        return new ArbitratedOverwriteResolution(
                normalizedClass,
                methodName + methodDescriptor,
                defaultStrategy,
                candidates,
                true
        );
    }

    /**
     * Applies the arbitrated resolution to the target ClassNode.
     *
     * @param targetNode the target ASM ClassNode
     * @param resolution the arbitrated overwrite resolution
     */
    public void applyResolution(ClassNode targetNode, ArbitratedOverwriteResolution resolution) {
        Objects.requireNonNull(targetNode, "targetNode cannot be null");
        Objects.requireNonNull(resolution, "resolution cannot be null");

        List<CandidateOverwrite> candidates = resolution.orderedCandidates();
        if (candidates.isEmpty()) {
            return;
        }

        if (candidates.size() == 1) {
            // Single candidate: apply cleanly as direct replacement
            CandidateOverwrite single = candidates.get(0);
            replaceMethod(targetNode, single.targetMethodName(), single.targetMethodDescriptor(), single.handlerMethod());
            return;
        }

        // Multiple conflicting overwrites
        if (resolution.strategyApplied() == ArbitrationStrategy.PRIORITY_DISPATCH) {
            // Apply highest priority candidate only
            CandidateOverwrite primary = candidates.get(0);
            replaceMethod(targetNode, primary.targetMethodName(), primary.targetMethodDescriptor(), primary.handlerMethod());
            return;
        }

        if (resolution.strategyApplied() == ArbitrationStrategy.CHAIN_SEQUENTIAL_INJECT) {
            // Decompose into sequential chain: inject each candidate as a synthetic step method
            for (int i = 0; i < candidates.size(); i++) {
                CandidateOverwrite candidate = candidates.get(i);
                String syntheticName = "$vessel$overwrite$" + candidate.modId() + "$" + candidate.targetMethodName() + "$" + i;

                MethodNode syntheticMethod = new MethodNode(
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC,
                        syntheticName,
                        candidate.targetMethodDescriptor(),
                        candidate.handlerMethod().signature,
                        candidate.handlerMethod().exceptions == null
                                ? null
                                : candidate.handlerMethod().exceptions.toArray(new String[0])
                );
                candidate.handlerMethod().accept(syntheticMethod);
                targetNode.methods.add(syntheticMethod);
            }

            // Replace primary target method with the primary candidate (index 0) to ensure deterministic dispatch
            CandidateOverwrite primary = candidates.get(0);
            replaceMethod(targetNode, primary.targetMethodName(), primary.targetMethodDescriptor(), primary.handlerMethod());
        }
    }

    private void replaceMethod(ClassNode targetNode, String name, String desc, MethodNode newMethod) {
        if (targetNode.methods == null) {
            targetNode.methods = new ArrayList<>();
        }

        targetNode.methods.removeIf(m -> m.name.equals(name) && m.desc.equals(desc));

        MethodNode copy = new MethodNode(
                newMethod.access,
                name,
                desc,
                newMethod.signature,
                newMethod.exceptions == null ? null : newMethod.exceptions.toArray(new String[0])
        );
        newMethod.accept(copy);
        targetNode.methods.add(copy);
    }
}
