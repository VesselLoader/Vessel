package dev.vessel.bootstrap.wave6;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.bootstrap.wave4.SampleMixinAccessorAndInjectFixture;
import dev.vessel.kernel.compat.era.MultiEraBytecodeTransformationPipeline;
import dev.vessel.kernel.identity.SourceLoaderKind;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Integration tests for Wave 6 verifying {@code MixinInjectAndUniqueMemberMerger} and
 * {@code RuntimeMixinTargetRegistry} (`v1.0.1`) merging {@code @Inject},
 * {@code @ModifyReturnValue}, and {@code @Unique} members directly into target classes during
 * {@link MultiEraBytecodeTransformationPipeline} execution.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveSixMixinMethodMergerWeaverTest {

    @Test
    void testMixinInjectAndUniqueMethodsAreMergedDirectlyIntoTargetBytecode() throws Exception {
        MultiEraBytecodeTransformationPipeline pipeline =
                new MultiEraBytecodeTransformationPipeline();

        String resourcePath =
                SampleMixinAccessorAndInjectFixture.class.getName().replace('.', '/') + ".class";
        byte[] sampleMixinBytes;
        try (InputStream in =
                SampleMixinAccessorAndInjectFixture.class
                        .getClassLoader()
                        .getResourceAsStream(resourcePath)) {
            assertNotNull(in);
            sampleMixinBytes = in.readAllBytes();
        }

        pipeline.transformLowerVersionClassForLatestServer(
                sampleMixinBytes,
                SourceLoaderKind.FABRIC,
                "1.20.4",
                "1.21.11");

        byte[] levelTargetBytes = synthesizeLevelTargetClass();
        byte[] mergedLevelBytes = pipeline.transformLowerVersionClassForLatestServer(
                levelTargetBytes,
                SourceLoaderKind.FABRIC,
                "1.20.4",
                "1.21.11");

        ClassNode levelNode = new ClassNode();
        new ClassReader(mergedLevelBytes).accept(levelNode, 0);
        Set<String> levelMethodNames = collectMethodNames(levelNode);
        assertTrue(levelMethodNames.contains("onSetBlock"));
        assertTrue(levelMethodNames.contains("modifyDayTime"));
        assertTrue(levelMethodNames.contains("wrapTick"));

        byte[] serverMixinBytes = synthesizeMinecraftServerInjectMixin();
        pipeline.transformLowerVersionClassForLatestServer(
                serverMixinBytes,
                SourceLoaderKind.FABRIC,
                "1.20.4",
                "1.21.11");

        byte[] serverTargetBytes = synthesizeMinecraftServerTargetClass();
        byte[] mergedServerBytes = pipeline.transformLowerVersionClassForLatestServer(
                serverTargetBytes,
                SourceLoaderKind.FABRIC,
                "1.20.4",
                "1.21.11");

        ClassNode serverNode = new ClassNode();
        new ClassReader(mergedServerBytes).accept(serverNode, 0);
        Set<String> serverMethodNames = collectMethodNames(serverNode);
        assertTrue(serverMethodNames.contains("vessel$onHeadTick"));
        assertTrue(serverMethodNames.contains("vessel$modifyMaxTickTime"));

        FieldNode uniqueCounterField = null;
        for (FieldNode fn : serverNode.fields) {
            if ("vessel$uniqueTickCounter".equals(fn.name)) {
                uniqueCounterField = fn;
                break;
            }
        }
        assertNotNull(uniqueCounterField);
        assertTrue((uniqueCounterField.access & Opcodes.ACC_PUBLIC) != 0);
    }

    private static Set<String> collectMethodNames(ClassNode node) {
        Set<String> names = new HashSet<>();
        for (MethodNode mn : node.methods) {
            names.add(mn.name);
        }
        return names;
    }

    private static byte[] synthesizeLevelTargetClass() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "net/minecraft/world/level/Level",
                null,
                "java/lang/Object",
                null);

        FieldVisitor fv = cw.visitField(
                Opcodes.ACC_PRIVATE,
                "isClientSide",
                "Z",
                null,
                null);
        fv.visitEnd();

        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PRIVATE,
                "tickBlockEntities",
                "()V",
                null,
                null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] synthesizeMinecraftServerInjectMixin() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "dev/vessel/bootstrap/wave6/SyntheticServerInjectMixin",
                null,
                "java/lang/Object",
                null);

        AnnotationVisitor mixinAv =
                cw.visitAnnotation("Lorg/spongepowered/asm/mixin/Mixin;", true);
        AnnotationVisitor targetsAv = mixinAv.visitArray("targets");
        targetsAv.visit(null, "net.minecraft.server.MinecraftServer");
        targetsAv.visitEnd();
        mixinAv.visitEnd();

        FieldVisitor shadowFv = cw.visitField(
                Opcodes.ACC_PRIVATE,
                "nextTickTimeNanos",
                "J",
                null,
                null);
        shadowFv.visitAnnotation("Lorg/spongepowered/asm/mixin/Shadow;", true).visitEnd();
        shadowFv.visitEnd();

        FieldVisitor uniqueFv = cw.visitField(
                Opcodes.ACC_PRIVATE,
                "vessel$uniqueTickCounter",
                "I",
                null,
                null);
        uniqueFv.visitAnnotation("Lorg/spongepowered/asm/mixin/Unique;", true).visitEnd();
        uniqueFv.visitEnd();

        MethodVisitor injectMv = cw.visitMethod(
                Opcodes.ACC_PRIVATE,
                "vessel$onHeadTick",
                "()V",
                null,
                null);
        AnnotationVisitor injectAv = injectMv.visitAnnotation(
                "Lorg/spongepowered/asm/mixin/injection/Inject;", true);
        injectAv.visit("method", "tickServer");
        injectAv.visitEnd();
        injectMv.visitCode();
        injectMv.visitInsn(Opcodes.RETURN);
        injectMv.visitMaxs(0, 1);
        injectMv.visitEnd();

        MethodVisitor modifyMv = cw.visitMethod(
                Opcodes.ACC_PRIVATE,
                "vessel$modifyMaxTickTime",
                "(J)J",
                null,
                null);
        AnnotationVisitor modifyAv = modifyMv.visitAnnotation(
                "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;", true);
        modifyAv.visit("method", "getMaxTickTime");
        modifyAv.visitEnd();
        modifyMv.visitCode();
        modifyMv.visitVarInsn(Opcodes.LLOAD, 1);
        modifyMv.visitInsn(Opcodes.LRETURN);
        modifyMv.visitMaxs(2, 3);
        modifyMv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] synthesizeMinecraftServerTargetClass() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "net/minecraft/server/MinecraftServer",
                null,
                "java/lang/Object",
                null);

        FieldVisitor fv = cw.visitField(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                "nextTickTimeNanos",
                "J",
                null,
                null);
        fv.visitEnd();

        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PRIVATE,
                "tickServer",
                "()V",
                null,
                null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
