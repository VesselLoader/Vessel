package dev.vessel.kernel.remap;

import dev.vessel.kernel.bootstrap.ScannedClassSymbols;
import dev.vessel.kernel.ir.VesselAccessLevel;
import dev.vessel.kernel.ir.VesselAccessWideningTable;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive verification suite for Sub-Stage 6.1 ({@code dev.vessel.kernel.remap}).
 *
 * <p>Uses pure in-memory ASM 9.7 bytecode synthesis with zero external downloads.</p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-27)
 */
class MultiNamespaceRemapperTest {

    @Test
    void shouldLoadJsonDictionaryAndRemapIntermediaryBytecodeAndReflectionLiterals() {
        CompactMappingDictionary dictionary = new CompactMappingDictionary();
        String json = """
                {
                  "classes": {
                    "net/minecraft/class_1937": "net/minecraft/world/level/Level",
                    "net/minecraft/class_1297": "net/minecraft/world/entity/Entity"
                  },
                  "methods": {
                    "net/minecraft/class_1937::method_8649::(Lnet/minecraft/class_1297;)Z": "addFreshEntity::(Lnet/minecraft/world/entity/Entity;)Z"
                  },
                  "fields": {
                    "net/minecraft/class_1937::field_9236::Z": "isClientSide::Z"
                  }
                }
                """;
        dictionary.loadFromJson(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                MappingNamespaceKind.INTERMEDIARY,
                MappingNamespaceKind.MOJMAP
        );

        assertThat(MappingNamespaceKind.INTERMEDIARY.requiresRuntimeTranslation(767)).isTrue();
        assertThat(MappingNamespaceKind.MOJMAP.requiresRuntimeTranslation(767)).isFalse();
        assertThat(MappingLookupResult.unmapped("foo", "()V").found()).isFalse();

        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/example/TestMod", null, "net/minecraft/class_1937", null);
        cw.visitField(Opcodes.ACC_PUBLIC, "field_9236", "Z", null, null).visitEnd();

        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "method_8649",
                "(Lnet/minecraft/class_1297;)Z",
                null,
                null
        );
        mv.visitCode();
        mv.visitLdcInsn("net.minecraft.class_1937");
        mv.visitInsn(Opcodes.POP);
        mv.visitLdcInsn("method_8649");
        mv.visitInsn(Opcodes.POP);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, "net/minecraft/class_1937", "field_9236", "Z");
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
        cw.visitEnd();

        ZeroCopyBytecodeRemapper remapper = new ZeroCopyBytecodeRemapper();
        byte[] remappedBytes = remapper.remapClassBytes(cw.toByteArray(), dictionary);

        ClassNode remappedNode = new ClassNode();
        new ClassReader(remappedBytes).accept(remappedNode, 0);

        assertThat(remappedNode.superName).isEqualTo("net/minecraft/world/level/Level");
        assertThat(remappedNode.fields).hasSize(1);
        assertThat(remappedNode.fields.get(0).name).isEqualTo("isClientSide");
        assertThat(remappedNode.methods).hasSize(1);

        MethodNode m = remappedNode.methods.get(0);
        assertThat(m.name).isEqualTo("addFreshEntity");
        assertThat(m.desc).isEqualTo("(Lnet/minecraft/world/entity/Entity;)Z");

        String indyHandle = remapper.mapInvokeDynamicHandle(
                "net/minecraft/class_1937",
                "method_8649",
                "(Lnet/minecraft/class_1297;)Z",
                dictionary
        );
        assertThat(indyHandle).isEqualTo("net/minecraft/world/level/Level.addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z");

        List<String> ldcStrings = m.instructions.toArray() != null
                ? java.util.Arrays.stream(m.instructions.toArray())
                .filter(LdcInsnNode.class::isInstance)
                .map(LdcInsnNode.class::cast)
                .map(n -> String.valueOf(n.cst))
                .toList()
                : List.of();
        assertThat(ldcStrings).containsExactly("net.minecraft.world.level.Level", "addFreshEntity");
    }

    @Test
    void shouldConvertForgeAtConfigAndCheckGetterRedirects() {
        String atCfg = """
                # Forge Access Transformer sample
                public-f net.minecraft.world.level.Level
                protected net.minecraft.world.level.Level f_46443_ # isClientSide
                public-f net.minecraft.server.MinecraftServer m_130011_()V
                """;
        AccessTransformerToWidenerConverter converter = new AccessTransformerToWidenerConverter();
        VesselAccessWideningTable table = converter.convertForgeAtConfig(
                new ByteArrayInputStream(atCfg.getBytes(StandardCharsets.UTF_8))
        );

        assertThat(table.getClassRule("net/minecraft/world/level/Level").accessLevel())
                .isEqualTo(VesselAccessLevel.PUBLIC);
        assertThat(table.getClassRule("net/minecraft/world/level/Level").extendable()).isTrue();
        assertThat(table.getFieldRule("net/minecraft/world/level/Level", "f_46443_").accessLevel())
                .isEqualTo(VesselAccessLevel.PROTECTED);
        assertThat(table.getMethodRule("net/minecraft/server/MinecraftServer", "m_130011_", "()V").extendable())
                .isTrue();

        converter.applyExtendableClassRule("net.minecraft.world.item.Item", table);
        assertThat(table.getClassRule("net/minecraft/world/item/Item").extendable()).isTrue();
        assertThat(converter.isFieldRedirectedToGetter("net.minecraft.world.entity.Entity", "level")).isTrue();
        assertThat(converter.isFieldRedirectedToGetter("net.minecraft.world.entity.Entity", "unknownField")).isFalse();
    }

    @Test
    void shouldStripClientAnnotatedMembersAndOrphanedSyntheticLambdas() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC,
                "com/example/DualMod",
                null,
                "java/lang/Object",
                new String[]{"net/fabricmc/api/ClientModInitializer", "java/lang/Runnable"}
        );

        MethodVisitor clientMethod = cw.visitMethod(Opcodes.ACC_PUBLIC, "onInitializeClient", "()V", null, null);
        AnnotationVisitor av = clientMethod.visitAnnotation("Lnet/fabricmc/api/Environment;", true);
        av.visitEnum("value", "Lnet/fabricmc/api/EnvType;", "CLIENT");
        av.visitEnd();
        clientMethod.visitCode();
        Handle bsm = new Handle(
                Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                        + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                        + "Ljava/lang/invoke/CallSite;",
                false
        );
        Handle lambdaTarget = new Handle(
                Opcodes.H_INVOKESTATIC,
                "com/example/DualMod",
                "lambda$onInitializeClient$0",
                "()V",
                false
        );
        clientMethod.visitInvokeDynamicInsn("run", "()Ljava/lang/Runnable;", bsm, lambdaTarget);
        clientMethod.visitInsn(Opcodes.POP);
        clientMethod.visitInsn(Opcodes.RETURN);
        clientMethod.visitMaxs(1, 1);
        clientMethod.visitEnd();

        MethodVisitor syntheticLambda = cw.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                "lambda$onInitializeClient$0",
                "()V",
                null,
                null
        );
        syntheticLambda.visitCode();
        syntheticLambda.visitInsn(Opcodes.RETURN);
        syntheticLambda.visitMaxs(0, 0);
        syntheticLambda.visitEnd();

        MethodVisitor serverMethod = cw.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        serverMethod.visitCode();
        serverMethod.visitInsn(Opcodes.RETURN);
        serverMethod.visitMaxs(0, 1);
        serverMethod.visitEnd();
        cw.visitEnd();

        ClientOnlySideStripper stripper = new ClientOnlySideStripper();
        ScannedClassSymbols clientSymbols = new ScannedClassSymbols(
                "com/example/SodiumClientMod",
                "java/lang/Object",
                List.of("net/fabricmc/api/ClientModInitializer"),
                Set.of("Lnet/fabricmc/api/Environment;"),
                List.of(),
                List.of(),
                false,
                false
        );
        assertThat(stripper.isClientOnlyModArchive(clientSymbols, "client")).isTrue();

        byte[] stripped = stripper.stripAnnotatedClientMembers(cw.toByteArray());
        ClassNode node = new ClassNode();
        new ClassReader(stripped).accept(node, 0);

        assertThat(node.interfaces).containsExactly("java/lang/Runnable");
        assertThat(node.methods).extracting(m -> m.name).containsExactly("run");

        String services = """
                # Service providers
                com.example.DualModClient
                com.example.ServerService
                """;
        assertThat(stripper.filterMetaInfServices(services, Set.of("com/example/DualModClient")))
                .isEqualTo("com.example.ServerService");
    }

    @Test
    void shouldStubClientRenderCallsAndFilterServerSafeEntrypoints() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/simibubi/create/KineticTile", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "tickVisuals", "(Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.FCONST_1);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/mojang/blaze3d/vertex/PoseStack",
                "scale",
                "(F)V",
                false
        );
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
        cw.visitEnd();

        DualSidedModEnvironmentSplitter splitter = new DualSidedModEnvironmentSplitter();
        byte[] serverSafeBytes = splitter.stubClientRenderCallsOnServer(cw.toByteArray());

        ClassNode node = new ClassNode();
        new ClassReader(serverSafeBytes).accept(node, 0);
        MethodNode tickVisuals = node.methods.get(0);
        boolean hasPoseStackCall = false;
        for (AbstractInsnNode insn = tickVisuals.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode mInsn && mInsn.owner.contains("PoseStack")) {
                hasPoseStackCall = true;
            }
        }
        assertThat(hasPoseStackCall).isFalse();

        List<String> filtered = splitter.filterServerSafeEntrypoints(List.of(
                "com.simibubi.create.Create",
                "com.simibubi.create.CreateClient",
                "com.simibubi.create.foundation.ponder.PonderIndex"
        ));
        assertThat(filtered).containsExactly("com.simibubi.create.Create");
    }

    @Test
    void shouldSynthesizeHybridOverloadsAndPreserveLvtIndices() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "net/minecraft/server/level/ServerLevel", null, "java/lang/Object", null);

        MethodVisitor addEntityMv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "addFreshEntity",
                "(Lnet/minecraft/world/entity/Entity;Lorg/bukkit/event/entity/CreatureSpawnEvent$SpawnReason;)Z",
                null,
                null
        );
        addEntityMv.visitCode();
        addEntityMv.visitInsn(Opcodes.ICONST_1);
        addEntityMv.visitInsn(Opcodes.IRETURN);
        addEntityMv.visitMaxs(1, 3);
        addEntityMv.visitEnd();

        MethodVisitor hurtMv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "hurt",
                "(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                null,
                null
        );
        Label l0 = new Label();
        Label l1 = new Label();
        hurtMv.visitCode();
        hurtMv.visitLabel(l0);
        hurtMv.visitInsn(Opcodes.ICONST_1);
        hurtMv.visitInsn(Opcodes.IRETURN);
        hurtMv.visitLabel(l1);
        hurtMv.visitLocalVariable("amount", "F", null, l0, l1, 2);
        hurtMv.visitMaxs(1, 3);
        hurtMv.visitEnd();
        cw.visitEnd();

        HybridMethodOverloadAndBridgeSynthesizer synthesizer = new HybridMethodOverloadAndBridgeSynthesizer();
        byte[] enhanced = synthesizer.synthesizeMissingOverloads(
                "net/minecraft/server/level/ServerLevel",
                cw.toByteArray()
        );

        ClassNode node = new ClassNode();
        new ClassReader(enhanced).accept(node, 0);

        List<String> methodSignatures = node.methods.stream()
                .map(m -> m.name + m.desc)
                .toList();

        assertThat(methodSignatures).contains(
                "addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
                "getChunkIfLoaded(II)Lnet/minecraft/world/level/chunk/LevelChunk;",
                "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;",
                "openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;",
                "teleportAsync(DDD)Ljava/util/concurrent/CompletableFuture;",
                "getCapability(Ljava/lang/Object;)Ljava/lang/Object;",
                "getEnchantPowerBonus()F",
                "getBurnTime()I",
                "getFriction()F"
        );

        MethodNode hurtNode = node.methods.stream()
                .filter(m -> "hurt".equals(m.name))
                .findFirst()
                .orElseThrow();
        assertThat(hurtNode.localVariables).hasSize(3);
        assertThat(hurtNode.localVariables.get(0).index).isEqualTo(0);
        assertThat(hurtNode.localVariables.get(1).index).isEqualTo(1);
        assertThat(hurtNode.localVariables.get(2).index).isEqualTo(2);
    }

    @Test
    void shouldHarmonizeReflectionToMethodHandlesAndInterceptRawThreads() throws InterruptedException {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/example/ReflectiveThreadMod", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "execute", "(Ljava/lang/reflect/Method;Ljava/lang/Thread;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/reflect/Method",
                "invoke",
                "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
                false
        );
        mv.visitInsn(Opcodes.POP);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "start", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
        cw.visitEnd();

        ReflectionCallCacheJitHarmonizer jitHarmonizer = new ReflectionCallCacheJitHarmonizer();
        byte[] harmonizedBytes = jitHarmonizer.rewriteReflectiveCallsToMethodHandles(cw.toByteArray());
        assertThat(jitHarmonizer.getCachedCallSiteCount()).isEqualTo(1);

        MethodHandle handle = jitHarmonizer.resolveAndCacheReflectiveTarget(
                "java/lang/String",
                "length",
                "()I",
                false
        );
        assertThat(handle).isNotNull();
        assertThat(jitHarmonizer.getCachedCallSiteCount()).isEqualTo(2);

        ThreadCreationInterceptionTrap threadTrap = new ThreadCreationInterceptionTrap();
        byte[] trappedBytes = threadTrap.interceptRawThreadInstantiations(harmonizedBytes);
        assertThat(threadTrap.getInterceptedThreadCount("com/example/ReflectiveThreadMod")).isGreaterThanOrEqualTo(1);

        ClassNode trappedNode = new ClassNode();
        new ClassReader(trappedBytes).accept(trappedNode, 0);

        CountDownLatch latch = new CountDownLatch(1);
        Thread vt = threadTrap.startInterceptedVirtualThread("create_mod", latch::countDown);
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(vt.isVirtual()).isTrue();
        assertThat(threadTrap.getInterceptedThreadCount("create_mod")).isEqualTo(1);
    }
}
