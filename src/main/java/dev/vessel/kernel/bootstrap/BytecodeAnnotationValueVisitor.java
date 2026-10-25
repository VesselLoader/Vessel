package dev.vessel.kernel.bootstrap;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Objects;

/**
 * Event-driven ASM AnnotationVisitor for scanning annotation array and scalar values
 * without classloading or nested inner classes.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BytecodeAnnotationValueVisitor extends AnnotationVisitor {

    private final List<String> targetList;

    /**
     * Constructs a BytecodeAnnotationValueVisitor that collects string or Type values into the given list.
     *
     * @param targetList list to populate with collected string values
     */
    public BytecodeAnnotationValueVisitor(List<String> targetList) {
        super(Opcodes.ASM9);
        this.targetList = Objects.requireNonNull(targetList, "targetList cannot be null");
    }

    @Override
    public void visit(String name, Object value) {
        if (value instanceof String str) {
            targetList.add(str.replace('.', '/'));
        } else if (value instanceof Type type) {
            targetList.add(type.getInternalName());
        }
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        return this;
    }
}
