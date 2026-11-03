package dev.vessel.bootstrap.wave8;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.vessel.kernel.compat.era.MultiEraBytecodeTransformationPipeline;
import dev.vessel.kernel.identity.SourceLoaderKind;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Integration tests for Wave 8 verifying {@code MixinCallbackInfoCancellableInliner} and
 * {@code MixinInjectAndUniqueMemberMerger} (`v1.0.2`) inlining local {@code CallbackInfo}
 * instantiation and early-return conditional branch (`isCancelled() -> RETURN`) into target
 * bytecode and executing it at runtime.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveEightMixinCallbackInfoCancellableInlinerTest {

    @Test
    void testCancellableCallbackInfoInjectHookCancelsTargetMethodExecutionAtRuntime()
            throws Exception {
        MultiEraBytecodeTransformationPipeline pipeline =
                new MultiEraBytecodeTransformationPipeline();

        String resourcePath =
                SampleCancellableCallbackInfoMixinFixture.class.getName().replace('.', '/')
                        + ".class";
        byte[] mixinBytes;
        try (InputStream in =
                SampleCancellableCallbackInfoMixinFixture.class
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

        byte[] rawTargetBytes = synthesizeCancellableTargetClass();
        byte[] transformedBytes = pipeline.transformLowerVersionClassForLatestServer(
                rawTargetBytes,
                SourceLoaderKind.FABRIC,
                "1.20.4",
                "1.21.11");

        Class<?> definedTargetClass = MethodHandles.lookup().defineClass(transformedBytes);
        Object targetInstance = definedTargetClass.getDeclaredConstructor().newInstance();

        Field executedCountField = definedTargetClass.getField("executedCount");
        Field cancelExecutionField = definedTargetClass.getField("cancelExecution");
        Method executeActionMethod = definedTargetClass.getMethod("executeAction");

        assertEquals(0, executedCountField.getInt(targetInstance));

        cancelExecutionField.setBoolean(targetInstance, false);
        executeActionMethod.invoke(targetInstance);
        assertEquals(1, executedCountField.getInt(targetInstance));

        cancelExecutionField.setBoolean(targetInstance, true);
        executeActionMethod.invoke(targetInstance);
        assertEquals(1, executedCountField.getInt(targetInstance));
    }

    private static byte[] synthesizeCancellableTargetClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "dev/vessel/bootstrap/wave8/SyntheticCancellableTarget",
                null,
                "java/lang/Object",
                null);

        FieldVisitor fv = cw.visitField(
                Opcodes.ACC_PUBLIC,
                "executedCount",
                "I",
                null,
                null);
        fv.visitEnd();

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

        MethodVisitor actionMv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "executeAction",
                "()V",
                null,
                null);
        actionMv.visitCode();
        actionMv.visitVarInsn(Opcodes.ALOAD, 0);
        actionMv.visitInsn(Opcodes.DUP);
        actionMv.visitFieldInsn(
                Opcodes.GETFIELD,
                "dev/vessel/bootstrap/wave8/SyntheticCancellableTarget",
                "executedCount",
                "I");
        actionMv.visitInsn(Opcodes.ICONST_1);
        actionMv.visitInsn(Opcodes.IADD);
        actionMv.visitFieldInsn(
                Opcodes.PUTFIELD,
                "dev/vessel/bootstrap/wave8/SyntheticCancellableTarget",
                "executedCount",
                "I");
        actionMv.visitInsn(Opcodes.RETURN);
        actionMv.visitMaxs(3, 1);
        actionMv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
