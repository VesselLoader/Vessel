package dev.vessel.kernel.sanitizer;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Objects;

/**
 * Generator producing synthetic class bytecode containing targeted constant strings for testing.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TestBytecodeClassGenerator {

    /**
     * Generates a basic compiled Java class containing a single constant string loaded in a dummy method.
     *
     * @param internalClassName slash-separated class name (e.g. "com/example/SampleClass")
     * @param constantString    string to embed as an LDC constant operand
     * @return compiled class bytes
     */
    public byte[] generateClassWithConstantString(String internalClassName, String constantString) {
        Objects.requireNonNull(internalClassName, "internalClassName cannot be null");
        Objects.requireNonNull(constantString, "constantString cannot be null");

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, internalClassName, null, "java/lang/Object", null);

        // Constructor <init>
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();

        // Sample method referencing the string
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getNotice", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitLdcInsn(constantString);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
