package dev.vessel.bootstrap.wave2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.kernel.compat.era.MultiEraBytecodeTransformationPipeline;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.remap.MappingNamespaceKind;
import dev.vessel.kernel.remap.runtime.BytecodeNamespaceClassifier;
import dev.vessel.kernel.remap.runtime.CanonicalMojMapSymbolRemapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Integration tests for Wave 2 verifying {@link BytecodeNamespaceClassifier},
 * {@link CanonicalMojMapSymbolRemapper}, and {@link MultiEraBytecodeTransformationPipeline}
 * automatic translation of Intermediary/SRG class, method, and field symbols into canonical
 * {@code MojMap} bytecode.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveTwoRuntimeMultiNamespaceRemapperTest {

    @Test
    void testIntermediaryBytecodeClassificationAndRemappingToMojMap() {
        byte[] rawIntermediaryBytes = synthesizeIntermediaryProbeClass();

        BytecodeNamespaceClassifier classifier = new BytecodeNamespaceClassifier();
        assertEquals(
                MappingNamespaceKind.INTERMEDIARY,
                classifier.classifyClassBytecode(rawIntermediaryBytes));

        CanonicalMojMapSymbolRemapper remapper = new CanonicalMojMapSymbolRemapper();
        byte[] remappedBytes = remapper.remapToMojMapIfNecessary(rawIntermediaryBytes);
        String remappedPool = new String(remappedBytes, StandardCharsets.ISO_8859_1);

        assertTrue(remappedPool.contains("net/minecraft/world/level/Level"));
        assertTrue(remappedPool.contains("net/minecraft/core/BlockPos"));
        assertTrue(remappedPool.contains("net/minecraft/world/level/block/state/BlockState"));
        assertTrue(remappedPool.contains("getBlockState"));
        assertTrue(remappedPool.contains("isClientSide"));
        assertFalse(remappedPool.contains("net/minecraft/class_1937"));
        assertFalse(remappedPool.contains("method_8520"));
        assertFalse(remappedPool.contains("field_9236"));
        assertEquals(
                MappingNamespaceKind.MOJMAP,
                classifier.classifyClassBytecode(remappedBytes));

        MultiEraBytecodeTransformationPipeline pipeline =
                new MultiEraBytecodeTransformationPipeline();
        byte[] pipelineTransformed = pipeline.transformLowerVersionClassForLatestServer(
                rawIntermediaryBytes,
                SourceLoaderKind.FABRIC,
                "1.20.1",
                "1.21.11");
        String pipelinePool = new String(pipelineTransformed, StandardCharsets.ISO_8859_1);

        assertTrue(pipelinePool.contains("net/minecraft/world/level/Level"));
        assertTrue(pipelinePool.contains("getBlockState"));
        assertTrue(pipelinePool.contains("isClientSide"));
    }

    private static byte[] synthesizeIntermediaryProbeClass() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "dev/vessel/bootstrap/wave2/SyntheticIntermediaryProbe",
                null,
                "java/lang/Object",
                null);

        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "probeLevel",
                "(Lnet/minecraft/class_1937;Lnet/minecraft/class_2338;)Lnet/minecraft/class_2680;",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                "net/minecraft/class_1937",
                "field_9236",
                "Z");
        mv.visitInsn(Opcodes.POP);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/class_1937",
                "method_8520",
                "(Lnet/minecraft/class_2338;)Lnet/minecraft/class_2680;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(2, 3);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
