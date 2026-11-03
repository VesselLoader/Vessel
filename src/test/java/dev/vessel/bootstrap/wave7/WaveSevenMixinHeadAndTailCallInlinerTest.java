package dev.vessel.bootstrap.wave7;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.kernel.compat.era.MultiEraBytecodeTransformationPipeline;
import dev.vessel.kernel.identity.SourceLoaderKind;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Integration tests for Wave 7 verifying {@code MixinHeadAndTailInstructionInliner} and
 * {@code MixinInjectAndUniqueMemberMerger} (`v1.0.1`) inlining a direct call to no-arg
 * {@code @Inject(at = @At("HEAD"))} hooks into target method bytecode.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveSevenMixinHeadAndTailCallInlinerTest {

    @Test
    void testNoArgHeadInjectHookIsInlinedIntoTargetMethodBytecode() throws Exception {
        MultiEraBytecodeTransformationPipeline pipeline =
                new MultiEraBytecodeTransformationPipeline();

        String resourcePath =
                SampleHeadInjectMixinFixture.class.getName().replace('.', '/') + ".class";
        byte[] mixinBytes;
        try (InputStream in =
                SampleHeadInjectMixinFixture.class
                        .getClassLoader()
                        .getResourceAsStream(resourcePath)) {
            assertNotNull(in);
            mixinBytes = in.readAllBytes();
        }

        pipeline.transformLowerVersionClassForLatestServer(
                mixinBytes,
                SourceLoaderKind.FABRIC,
                "1.20.4",
                "1.21.11");

        byte[] targetBytes = synthesizeTickTargetClass();
        byte[] transformedTargetBytes = pipeline.transformLowerVersionClassForLatestServer(
                targetBytes,
                SourceLoaderKind.FABRIC,
                "1.20.4",
                "1.21.11");

        ClassNode targetNode = new ClassNode();
        new ClassReader(transformedTargetBytes).accept(targetNode, 0);

        MethodNode runTickMethod = null;
        MethodNode mergedHeadHook = null;
        for (MethodNode mn : targetNode.methods) {
            if ("runTick".equals(mn.name) && "()V".equals(mn.desc)) {
                runTickMethod = mn;
            } else if ("vessel$onRunTickHead".equals(mn.name) && "()V".equals(mn.desc)) {
                mergedHeadHook = mn;
            }
        }
        assertNotNull(runTickMethod);
        assertNotNull(mergedHeadHook);

        boolean foundInlinedCallBeforeReturn = false;
        boolean seenReturn = false;
        for (AbstractInsnNode insn = runTickMethod.instructions.getFirst();
                insn != null;
                insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                seenReturn = true;
            }
            if (insn instanceof MethodInsnNode methodInsn
                    && methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL
                    && "dev/vessel/bootstrap/wave7/SyntheticTickTarget".equals(methodInsn.owner)
                    && "vessel$onRunTickHead".equals(methodInsn.name)
                    && "()V".equals(methodInsn.desc)
                    && !seenReturn) {
                foundInlinedCallBeforeReturn = true;
            }
        }
        assertTrue(foundInlinedCallBeforeReturn);
        assertTrue(seenReturn);
        assertEquals(
                "dev/vessel/bootstrap/wave7/SyntheticTickTarget",
                targetNode.name);
    }

    private static byte[] synthesizeTickTargetClass() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "dev/vessel/bootstrap/wave7/SyntheticTickTarget",
                null,
                "java/lang/Object",
                null);

        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "runTick",
                "()V",
                null,
                null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
