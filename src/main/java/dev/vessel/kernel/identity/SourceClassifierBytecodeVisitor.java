package dev.vessel.kernel.identity;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Objects;

/**
 * ASM ClassVisitor that inspects superclasses, interfaces, annotations, and methods
 * to accurately identify the originating ecosystem loader without loading classes into JVM.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SourceClassifierBytecodeVisitor extends ClassVisitor {

    private final SourceClassifierContext context;

    /**
     * Constructs a SourceClassifierBytecodeVisitor bound to the specified context.
     *
     * @param context classifier context
     */
    public SourceClassifierBytecodeVisitor(SourceClassifierContext context) {
        super(Opcodes.ASM9);
        this.context = Objects.requireNonNull(context, "context cannot be null");
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        context.recordTypeReference(superName);
        if (interfaces != null) {
            for (int i = 0; i < interfaces.length; i++) {
                context.recordTypeReference(interfaces[i]);
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        context.recordAnnotation(descriptor);
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new SourceClassifierMethodVisitor(context);
    }

    /**
     * Returns the associated classification context.
     *
     * @return classifier context
     */
    public SourceClassifierContext context() {
        return context;
    }
}
