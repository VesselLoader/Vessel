package dev.vessel.bootstrap.wave11;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.vessel.kernel.mixin.weaver.MixinInjectAndUniqueMemberMerger;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Integration test verifying {@code CallbackInfoReturnable} early return bytecode weaving at runtime.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveElevenMixinCallbackInfoReturnableTest {

    @Test
    void testCallbackInfoReturnableOverridesReturnValueAtRuntime() throws Exception {
        String resourcePath = SampleReturnableMixinFixture.class.getName().replace('.', '/') + ".class";
        byte[] mixinBytes;
        try (InputStream in = SampleReturnableMixinFixture.class.getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(in);
            mixinBytes = in.readAllBytes();
        }

        byte[] rawTargetBytes = synthesizeDefenseTargetClass();
        MixinInjectAndUniqueMemberMerger merger = new MixinInjectAndUniqueMemberMerger();
        byte[] transformedBytes = merger.mergeMixinMembersIntoTarget(rawTargetBytes, mixinBytes);

        Class<?> definedTargetClass = MethodHandles.lookup().defineClass(transformedBytes);
        Object targetInstance = definedTargetClass.getDeclaredConstructor().newInstance();
        Method defenseMethod = definedTargetClass.getMethod("computeBaseDefense");

        int result = (int) defenseMethod.invoke(targetInstance);
        assertEquals(999, result);
    }

    private static byte[] synthesizeDefenseTargetClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "dev/vessel/bootstrap/wave11/SyntheticDefenseTarget",
                null,
                "java/lang/Object",
                null);

        MethodVisitor initMv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null);
        initMv.visitCode();
        initMv.visitVarInsn(Opcodes.ALOAD, 0);
        initMv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false);
        initMv.visitInsn(Opcodes.RETURN);
        initMv.visitMaxs(1, 1);
        initMv.visitEnd();

        MethodVisitor defenseMv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "computeBaseDefense",
                "()I",
                null,
                null);
        defenseMv.visitCode();
        defenseMv.visitIntInsn(Opcodes.BIPUSH, 10);
        defenseMv.visitInsn(Opcodes.IRETURN);
        defenseMv.visitMaxs(1, 1);
        defenseMv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
