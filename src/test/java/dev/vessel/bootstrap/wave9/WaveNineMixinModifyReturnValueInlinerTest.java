package dev.vessel.bootstrap.wave9;

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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Integration tests for Wave 9 verifying {@code MixinModifyReturnValueInliner} and
 * {@code MixinInjectAndUniqueMemberMerger} (`v1.0.3`) inlining {@code @ModifyReturnValue} hooks
 * immediately before return instructions (`IRETURN`) and amplifying the returned value at runtime.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveNineMixinModifyReturnValueInlinerTest {

    @Test
    void testModifyReturnValueHookAmplifiesReturnedIntAtRuntime() throws Exception {
        MultiEraBytecodeTransformationPipeline pipeline =
                new MultiEraBytecodeTransformationPipeline();

        String resourcePath =
                SampleModifyReturnValueMixinFixture.class.getName().replace('.', '/') + ".class";
        byte[] mixinBytes;
        try (InputStream in =
                SampleModifyReturnValueMixinFixture.class
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

        byte[] rawTargetBytes = synthesizeDamageCalculatorTargetClass();
        byte[] transformedBytes = pipeline.transformLowerVersionClassForLatestServer(
                rawTargetBytes,
                SourceLoaderKind.FABRIC,
                "1.20.4",
                "1.21.11");

        Class<?> definedTargetClass = MethodHandles.lookup().defineClass(transformedBytes);
        Object calculatorInstance = definedTargetClass.getDeclaredConstructor().newInstance();

        Method computeBaseDamageMethod = definedTargetClass.getMethod("computeBaseDamage");
        Field bonusDamageField = definedTargetClass.getField("bonusDamage");

        int amplifiedResult = (int) computeBaseDamageMethod.invoke(calculatorInstance);
        assertEquals(25, amplifiedResult);

        bonusDamageField.setInt(calculatorInstance, 30);
        int customAmplifiedResult = (int) computeBaseDamageMethod.invoke(calculatorInstance);
        assertEquals(40, customAmplifiedResult);
    }

    private static byte[] synthesizeDamageCalculatorTargetClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "dev/vessel/bootstrap/wave9/SyntheticDamageCalculatorTarget",
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

        MethodVisitor computeMv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "computeBaseDamage",
                "()I",
                null,
                null);
        computeMv.visitCode();
        computeMv.visitIntInsn(Opcodes.BIPUSH, 10);
        computeMv.visitInsn(Opcodes.IRETURN);
        computeMv.visitMaxs(1, 1);
        computeMv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
