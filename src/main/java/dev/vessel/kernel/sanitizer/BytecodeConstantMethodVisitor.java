package dev.vessel.kernel.sanitizer;

import org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.Objects;

/**
 * ASM MethodVisitor inspecting constant LDC string operands to discover embedded license notices.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BytecodeConstantMethodVisitor extends MethodVisitor {

    private final List<String> extractedStrings;

    /**
     * Constructs the method visitor.
     *
     * @param api              ASM API version
     * @param methodVisitor    delegate method visitor
     * @param extractedStrings target collection accumulating discovered string constants
     */
    public BytecodeConstantMethodVisitor(int api, MethodVisitor methodVisitor, List<String> extractedStrings) {
        super(api, methodVisitor);
        this.extractedStrings = Objects.requireNonNull(extractedStrings, "extractedStrings cannot be null");
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof String str) {
            extractedStrings.add(str);
        }
        super.visitLdcInsn(value);
    }
}
