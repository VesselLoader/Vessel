package dev.vessel.kernel.sanitizer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.Objects;

/**
 * ASM ClassVisitor traversing class structures, annotations, source metadata,
 * and bytecode instructions to extract all literal strings for license auditing.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BytecodeStringExtractorClassVisitor extends ClassVisitor {

    private final List<String> extractedStrings;
    private String className;

    /**
     * Constructs the class visitor.
     *
     * @param extractedStrings target collection accumulating discovered strings
     */
    public BytecodeStringExtractorClassVisitor(List<String> extractedStrings) {
        super(Opcodes.ASM9);
        this.extractedStrings = Objects.requireNonNull(extractedStrings, "extractedStrings cannot be null");
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        if (name != null) {
            extractedStrings.add(name);
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        if (source != null) {
            extractedStrings.add(source);
        }
        if (debug != null) {
            extractedStrings.add(debug);
        }
        super.visitSource(source, debug);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new BytecodeConstantMethodVisitor(api, mv, extractedStrings);
    }

    /**
     * Retrieves the internal class name encountered during traversal.
     *
     * @return internal class name, or null if not yet visited
     */
    public String getClassName() {
        return className;
    }
}
