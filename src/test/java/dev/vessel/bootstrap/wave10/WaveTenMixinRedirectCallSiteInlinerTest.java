package dev.vessel.bootstrap.wave10;

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
 * Integration tests for Wave 10 verifying {@code MixinRedirectCallSiteInliner} and
 * {@link MixinInjectAndUniqueMemberMerger} (`v1.0.4`) redirecting target method call sites
 * annotated with {@code @Redirect(at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"))}
 * and executing the redirected bytecode at runtime.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveTenMixinRedirectCallSiteInlinerTest {

    @Test
    void testRedirectHookRewritesMathMaxCallSiteAtRuntime() throws Exception {
        String resourcePath =
                SampleRedirectMixinFixture.class.getName().replace('.', '/') + ".class";
        byte[] mixinBytes;
        try (InputStream in =
                SampleRedirectMixinFixture.class
                        .getClassLoader()
                        .getResourceAsStream(resourcePath)) {
            assertNotNull(in);
            mixinBytes = in.readAllBytes();
        }

        byte[] rawTargetBytes = synthesizeRedirectDamageTargetClass();
        MixinInjectAndUniqueMemberMerger merger = new MixinInjectAndUniqueMemberMerger();
        byte[] transformedBytes =
                merger.mergeMixinMembersIntoTarget(rawTargetBytes, mixinBytes);

        Class<?> definedTargetClass = MethodHandles.lookup().defineClass(transformedBytes);
        Object targetInstance = definedTargetClass.getDeclaredConstructor().newInstance();
        Method clampMethod = definedTargetClass.getMethod("clampDamageFloor", int.class);

        int clampedLowValue = (int) clampMethod.invoke(targetInstance, 3);
        assertEquals(10, clampedLowValue);

        int clampedHighValue = (int) clampMethod.invoke(targetInstance, 25);
        assertEquals(25, clampedHighValue);
    }

    private static byte[] synthesizeRedirectDamageTargetClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "dev/vessel/bootstrap/wave10/SyntheticRedirectDamageTarget",
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

        MethodVisitor clampMv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "clampDamageFloor",
                "(I)I",
                null,
                null);
        clampMv.visitCode();
        clampMv.visitVarInsn(Opcodes.ILOAD, 1);
        clampMv.visitInsn(Opcodes.ICONST_1);
        clampMv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Math",
                "max",
                "(II)I",
                false);
        clampMv.visitInsn(Opcodes.IRETURN);
        clampMv.visitMaxs(2, 2);
        clampMv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
