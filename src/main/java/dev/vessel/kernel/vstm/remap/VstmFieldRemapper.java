package dev.vessel.kernel.vstm.remap;

import java.util.Objects;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.Remapper;

/**
 * Bytecode field visitor that remaps declaration annotations and type annotations
 * associated with a field according to an ASM {@link Remapper}.
 *
 * <p>Field names, descriptors, and generic signatures are pre-remapped during
 * {@code ClassVisitor.visitField} prior to invoking this visitor (Catalog ref: USML-0021).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 3)
 */
public final class VstmFieldRemapper extends FieldVisitor {

    private final Remapper remapper;

    /**
     * Constructs a field remapper using the default ASM9 API version.
     *
     * @param fieldVisitor the delegate field visitor
     * @param remapper     the active remapper
     */
    public VstmFieldRemapper(FieldVisitor fieldVisitor, Remapper remapper) {
        this(Opcodes.ASM9, fieldVisitor, remapper);
    }

    /**
     * Constructs a field remapper with a specific ASM API version.
     *
     * @param api          the ASM API version
     * @param fieldVisitor the delegate field visitor
     * @param remapper     the active remapper
     */
    public VstmFieldRemapper(int api, FieldVisitor fieldVisitor, Remapper remapper) {
        super(api, fieldVisitor);
        this.remapper = Objects.requireNonNull(remapper, "remapper must not be null");
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        String mappedDesc = remapper.mapDesc(descriptor);
        AnnotationVisitor delegate = super.visitAnnotation(mappedDesc, visible);
        if (delegate == null) {
            return null;
        }
        return new VstmAnnotationRemapper(descriptor, delegate, remapper);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        String mappedDesc = remapper.mapDesc(descriptor);
        AnnotationVisitor delegate = super.visitTypeAnnotation(typeRef, typePath, mappedDesc, visible);
        if (delegate == null) {
            return null;
        }
        return new VstmAnnotationRemapper(descriptor, delegate, remapper);
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
