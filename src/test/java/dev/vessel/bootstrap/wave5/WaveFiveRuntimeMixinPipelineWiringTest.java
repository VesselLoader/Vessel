package dev.vessel.bootstrap.wave5;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.bootstrap.wave4.SampleMixinAccessorAndInjectFixture;
import dev.vessel.kernel.compat.era.MultiEraBytecodeTransformationPipeline;
import dev.vessel.kernel.identity.SourceLoaderKind;
import java.io.InputStream;
import java.util.List;
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
 * Integration tests for Wave 5 verifying {@link MultiEraBytecodeTransformationPipeline} (`v1.0.4`)
 * automatic {@code @Mixin} bytecode discovery via {@code RuntimeMixinTargetRegistry} and
 * load-time target member access widening.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveFiveRuntimeMixinPipelineWiringTest {

    @Test
    void testPipelineAutoDiscoversMixinAndWidensTargetBytecode() throws Exception {
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

        assertTrue(
                pipeline.getMixinTargetRegistry()
                        .isRegisteredTarget("net.minecraft.world.level.Level"));
        assertTrue(
                pipeline.getMixinTargetRegistry()
                        .getWidenedMembersForTarget("net/minecraft/world/level/Level")
                        .contains("isClientSide"));

        byte[] serverMixinBytes = synthesizeMinecraftServerMixinClass();
        pipeline.transformLowerVersionClassForLatestServer(
                serverMixinBytes,
                SourceLoaderKind.FABRIC,
                "1.20.4",
                "1.21.11");

        assertTrue(
                pipeline.getMixinTargetRegistry()
                        .isRegisteredTarget("net.minecraft.server.MinecraftServer"));
        Set<String> serverWidenedMembers =
                pipeline.getMixinTargetRegistry()
                        .getWidenedMembersForTarget("net/minecraft/server/MinecraftServer");
        assertTrue(serverWidenedMembers.contains("nextTickTimeNanos"));
        assertTrue(serverWidenedMembers.contains("tickServer"));

        byte[] rawTargetBytes = synthesizeMinecraftServerTargetClass();
        byte[] widenedTargetBytes = pipeline.transformLowerVersionClassForLatestServer(
                rawTargetBytes,
                SourceLoaderKind.FABRIC,
                "1.20.4",
                "1.21.11");

        ClassNode targetNode = new ClassNode();
        new ClassReader(widenedTargetBytes).accept(targetNode, 0);

        FieldNode nextTickField = null;
        for (FieldNode fn : targetNode.fields) {
            if ("nextTickTimeNanos".equals(fn.name)) {
                nextTickField = fn;
                break;
            }
        }
        assertNotNull(nextTickField);
        assertTrue((nextTickField.access & Opcodes.ACC_PUBLIC) != 0);
        assertFalse((nextTickField.access & Opcodes.ACC_PRIVATE) != 0);
        assertFalse((nextTickField.access & Opcodes.ACC_FINAL) != 0);

        MethodNode tickServerMethod = null;
        for (MethodNode mn : targetNode.methods) {
            if ("tickServer".equals(mn.name)) {
                tickServerMethod = mn;
                break;
            }
        }
        assertNotNull(tickServerMethod);
        assertTrue((tickServerMethod.access & Opcodes.ACC_PUBLIC) != 0);
        assertFalse((tickServerMethod.access & Opcodes.ACC_PRIVATE) != 0);
    }

    private static byte[] synthesizeMinecraftServerMixinClass() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "dev/vessel/bootstrap/wave5/SyntheticMinecraftServerMixin",
                null,
                "java/lang/Object",
                null);

        AnnotationVisitor mixinAv =
                cw.visitAnnotation("Lorg/spongepowered/asm/mixin/Mixin;", true);
        AnnotationVisitor targetsAv = mixinAv.visitArray("targets");
        targetsAv.visit(null, "net.minecraft.server.MinecraftServer");
        targetsAv.visitEnd();
        mixinAv.visitEnd();

        FieldVisitor fv = cw.visitField(
                Opcodes.ACC_PRIVATE,
                "nextTickTimeNanos",
                "J",
                null,
                null);
        fv.visitAnnotation("Lorg/spongepowered/asm/mixin/Shadow;", true).visitEnd();
        fv.visitEnd();

        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "tickServer",
                "()V",
                null,
                null);
        mv.visitAnnotation("Lorg/spongepowered/asm/mixin/gen/Invoker;", true).visitEnd();
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();

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
