package dev.vessel.kernel.vstm.remap;

import java.util.Objects;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;

/**
 * Bytecode annotation visitor that remaps annotation type descriptors, enum constants,
 * array elements, and nested annotations according to an ASM {@link Remapper}.
 *
 * <p>Operates cleanly on single-responsibility principles without inner classes (Rule 17).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 3)
 */
public final class VstmAnnotationRemapper extends AnnotationVisitor {

    private final String descriptor;
    private final Remapper remapper;

    /**
     * Constructs an annotation remapper without an explicit descriptor.
     *
     * @param annotationVisitor the parent annotation visitor delegate
     * @param remapper          the active remapper
     */
    public VstmAnnotationRemapper(AnnotationVisitor annotationVisitor, Remapper remapper) {
        this(null, annotationVisitor, remapper);
    }

    /**
     * Constructs an annotation remapper with an explicit annotation descriptor.
     *
     * @param descriptor        the original descriptor of the annotation
     * @param annotationVisitor the parent annotation visitor delegate
     * @param remapper          the active remapper
     */
    public VstmAnnotationRemapper(String descriptor, AnnotationVisitor annotationVisitor, Remapper remapper) {
        this(Opcodes.ASM9, descriptor, annotationVisitor, remapper);
    }

    /**
     * Constructs an annotation remapper with a specific ASM API version.
     *
     * @param api               the ASM API version
     * @param descriptor        the original descriptor of the annotation
     * @param annotationVisitor the parent annotation visitor delegate
     * @param remapper          the active remapper
     */
    public VstmAnnotationRemapper(int api, String descriptor, AnnotationVisitor annotationVisitor, Remapper remapper) {
        super(api, annotationVisitor);
        this.descriptor = descriptor;
        this.remapper = Objects.requireNonNull(remapper, "remapper must not be null");
    }

    @Override
    public void visit(String name, Object value) {
        Object remappedValue = remapper.mapValue(value);
        super.visit(name, remappedValue);
    }

    @Override
    public void visitEnum(String name, String enumDescriptor, String value) {
        String mappedDesc = remapper.mapDesc(enumDescriptor);
        String mappedValue = value;
        if (enumDescriptor != null && enumDescriptor.startsWith("L") && enumDescriptor.endsWith(";")) {
            String internalName = enumDescriptor.substring(1, enumDescriptor.length() - 1);
            String candidate = remapper.mapFieldName(internalName, value, enumDescriptor);
            if (candidate != null && !candidate.isEmpty()) {
                mappedValue = candidate;
            }
        }
        super.visitEnum(name, mappedDesc, mappedValue);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String nestedDescriptor) {
        String mappedDesc = remapper.mapDesc(nestedDescriptor);
        AnnotationVisitor childVisitor = super.visitAnnotation(name, mappedDesc);
        if (childVisitor == null) {
            return null;
        }
        return new VstmAnnotationRemapper(nestedDescriptor, childVisitor, remapper);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        AnnotationVisitor childVisitor = super.visitArray(name);
        if (childVisitor == null) {
            return null;
        }
        return new VstmAnnotationRemapper(descriptor, childVisitor, remapper);
    }

    /**
     * Retrieves the descriptor of the annotation being visited.
     *
     * @return the descriptor, or null if unknown
     */
    public String getDescriptor() {
        return descriptor;
    }

    /**
     * Retrieves the active remapper instance.
     *
     * @return the remapper
     */
    public Remapper getRemapper() {
        return remapper;
    }
}
