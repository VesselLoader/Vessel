package dev.vessel.kernel.transform.mixin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying Mixin conflict arbitration, wrap operation chain coordination,
 * and shadow field access validation.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MixinConflictArbitrationTest {

    @Test
    @DisplayName("Single @Overwrite candidate cleanly replaces target method")
    public void testSingleOverwriteReplacement() {
        MixinConflictArbitrationEngine engine = new MixinConflictArbitrationEngine();
        String targetClass = "net/minecraft/server/level/ServerPlayer";
        String methodName = "tick";
        String methodDesc = "()V";

        MethodNode handler = new MethodNode(Opcodes.ACC_PUBLIC, methodName, methodDesc, null, null);
        handler.instructions.add(new InsnNode(Opcodes.RETURN));

        CandidateOverwrite candidate = new CandidateOverwrite(
                "mod_a",
                "com/example/ModAMixin",
                methodName,
                methodDesc,
                1000,
                handler
        );
        engine.registerCandidate(targetClass, candidate);

        ArbitratedOverwriteResolution resolution = engine.arbitrate(targetClass, methodName, methodDesc);
        assertThat(resolution.resolvedSuccessfully()).isTrue();
        assertThat(resolution.orderedCandidates()).hasSize(1);

        ClassNode targetNode = new ClassNode();
        targetNode.name = targetClass;
        MethodNode originalMethod = new MethodNode(Opcodes.ACC_PUBLIC, methodName, methodDesc, null, null);
        targetNode.methods.add(originalMethod);

        engine.applyResolution(targetNode, resolution);

        assertThat(targetNode.methods).hasSize(1);
        assertThat(targetNode.methods.get(0).name).isEqualTo(methodName);
    }

    @Test
    @DisplayName("Conflicting @Overwrite candidates are arbitrated into sequential chain ordered by priority")
    public void testConflictingOverwriteSequentialChain() {
        MixinConflictArbitrationEngine engine = new MixinConflictArbitrationEngine(ArbitrationStrategy.CHAIN_SEQUENTIAL_INJECT);
        String targetClass = "net/minecraft/world/entity/Entity";
        String methodName = "baseTick";
        String methodDesc = "()V";

        MethodNode handler1 = new MethodNode(Opcodes.ACC_PUBLIC, methodName, methodDesc, null, null);
        handler1.instructions.add(new InsnNode(Opcodes.RETURN));
        CandidateOverwrite candidateLowPriority = new CandidateOverwrite(
                "mod_late",
                "com/example/ModLateMixin",
                methodName,
                methodDesc,
                2000,
                handler1
        );

        MethodNode handler2 = new MethodNode(Opcodes.ACC_PUBLIC, methodName, methodDesc, null, null);
        handler2.instructions.add(new InsnNode(Opcodes.RETURN));
        CandidateOverwrite candidateHighPriority = new CandidateOverwrite(
                "mod_early",
                "com/example/ModEarlyMixin",
                methodName,
                methodDesc,
                500,
                handler2
        );

        engine.registerCandidate(targetClass, candidateLowPriority);
        engine.registerCandidate(targetClass, candidateHighPriority);

        ArbitratedOverwriteResolution resolution = engine.arbitrate(targetClass, methodName, methodDesc);
        assertThat(resolution.orderedCandidates()).hasSize(2);
        // Priority 500 should precede priority 2000
        assertThat(resolution.orderedCandidates().get(0).modId()).isEqualTo("mod_early");
        assertThat(resolution.orderedCandidates().get(1).modId()).isEqualTo("mod_late");

        ClassNode targetNode = new ClassNode();
        targetNode.name = targetClass;

        engine.applyResolution(targetNode, resolution);

        // Should contain primary method and 2 synthetic step methods
        assertThat(targetNode.methods.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("WrapOperationCallChainCoordinator builds ordered handler chain and detects contested targets")
    public void testWrapOperationChainCoordinator() {
        WrapOperationCallChainCoordinator coordinator = new WrapOperationCallChainCoordinator();
        WrapOperationTarget target = new WrapOperationTarget(
                "net/minecraft/world/level/Level",
                "setBlock",
                "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/level/chunk/LevelChunk",
                "setBlockState",
                "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
                0
        );

        WrapOperationHandler handler1 = new WrapOperationHandler("mod_b", "com/b/Mixin", "wrapChunk", "()V", 1500);
        WrapOperationHandler handler2 = new WrapOperationHandler("mod_a", "com/a/Mixin", "wrapChunk", "()V", 800);

        coordinator.registerHandler(target, handler1);
        coordinator.registerHandler(target, handler2);

        WrapOperationChain chain = coordinator.buildChain(target);
        assertThat(chain.size()).isEqualTo(2);
        // Priority 800 executes before 1500
        assertThat(chain.handlers().get(0).modId()).isEqualTo("mod_a");
        assertThat(chain.handlers().get(1).modId()).isEqualTo("mod_b");

        assertThat(coordinator.getContestedTargets()).containsKey(target);
        assertThat(coordinator.getContestedTargets().get(target)).isEqualTo(2);
    }

    @Test
    @DisplayName("ShadowFieldAccessValidator verifies valid, missing, and mismatched shadow fields")
    public void testShadowFieldAccessValidator() {
        ShadowFieldAccessValidator validator = new ShadowFieldAccessValidator();
        ClassNode targetNode = new ClassNode();
        targetNode.name = "net/minecraft/world/entity/LivingEntity";
        targetNode.fields.add(new FieldNode(Opcodes.ACC_PROTECTED, "health", "F", null, null));
        targetNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, "deathScore", "I", null, null));

        // Valid match
        ShadowValidationResult validResult = validator.validate(
                targetNode,
                new ShadowFieldDescriptor(targetNode.name, "health", "F")
        );
        assertThat(validResult.isValid()).isTrue();
        assertThat(validResult.status()).isEqualTo(ShadowValidationStatus.VALID);

        // Missing field
        ShadowValidationResult missingResult = validator.validate(
                targetNode,
                new ShadowFieldDescriptor(targetNode.name, "nonExistentField", "Ljava/lang/String;")
        );
        assertThat(missingResult.isValid()).isFalse();
        assertThat(missingResult.status()).isEqualTo(ShadowValidationStatus.MISSING_FIELD);

        // Descriptor mismatch
        ShadowValidationResult mismatchResult = validator.validate(
                targetNode,
                new ShadowFieldDescriptor(targetNode.name, "health", "D") // expected double, found float F
        );
        assertThat(mismatchResult.isValid()).isFalse();
        assertThat(mismatchResult.status()).isEqualTo(ShadowValidationStatus.DESCRIPTOR_MISMATCH);
    }
}
