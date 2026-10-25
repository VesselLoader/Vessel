package dev.vessel.kernel.vstm.remap;

import java.util.Objects;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.Remapper;

/**
 * Top-level ASM ClassVisitor coordinating complete class-structure remapping.
 *
 * <p>Rewrites class names, superclasses, interfaces, generic signatures, inner class attributes,
 * record components, nest members, and delegates member rewriting to SRP micro-remapper visitors
 * (Catalog ref: USML-0021, FOL-0040).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 3)
 */
public final class VstmClassRemapper extends ClassVisitor {

    private final Remapper remapper;
    private String rawClassName;

    /**
     * Constructs a class remapper with default ASM9 API version.
     *
     * @param classVisitor downstream class visitor delegate (e.g. ClassWriter)
     * @param remapper     active bytecode remapper
     */
    public VstmClassRemapper(ClassVisitor classVisitor, Remapper remapper) {
        this(Opcodes.ASM9, classVisitor, remapper);
    }

    /**
     * Constructs a class remapper with an explicit ASM API version.
     *
     * @param api          ASM API version
     * @param classVisitor downstream class visitor delegate
     * @param remapper     active bytecode remapper
     */
    public VstmClassRemapper(int api, ClassVisitor classVisitor, Remapper remapper) {
        super(api, classVisitor);
        this.remapper = Objects.requireNonNull(remapper, "remapper must not be null");
    }

    @Override
    public void visit(
            int version,
            int access,
            String name,
            String signature,
            String superName,
            String[] interfaces
    ) {
        this.rawClassName = name;
        String mappedName = remapper.mapType(name);
        String mappedSig = (signature != null) ? remapper.mapSignature(signature, false) : null;
        String mappedSuper = (superName != null) ? remapper.mapType(superName) : null;
        String[] mappedInterfaces = (interfaces != null) ? remapper.mapTypes(interfaces) : null;

        super.visit(version, access, mappedName, mappedSig, mappedSuper, mappedInterfaces);
    }

    @Override
    public FieldVisitor visitField(
            int access,
            String name,
            String descriptor,
            String signature,
            Object value
    ) {
        String mappedName = remapper.mapFieldName(this.rawClassName, name, descriptor);
        String mappedDesc = remapper.mapDesc(descriptor);
        String mappedSig = (signature != null) ? remapper.mapSignature(signature, true) : null;
        Object mappedValue = remapper.mapValue(value);

        FieldVisitor delegate = super.visitField(
                access,
                mappedName != null ? mappedName : name,
                mappedDesc,
                mappedSig,
                mappedValue
        );
        if (delegate == null) {
            return null;
        }
        return new VstmFieldRemapper(delegate, remapper);
    }

    @Override
    public MethodVisitor visitMethod(
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions
    ) {
        String mappedName = remapper.mapMethodName(this.rawClassName, name, descriptor);
        String mappedDesc = remapper.mapMethodDesc(descriptor);
        String mappedSig = (signature != null) ? remapper.mapSignature(signature, false) : null;
        String[] mappedExceptions = (exceptions != null) ? remapper.mapTypes(exceptions) : null;

        MethodVisitor delegate = super.visitMethod(
                access,
                mappedName != null ? mappedName : name,
                mappedDesc,
                mappedSig,
                mappedExceptions
        );
        if (delegate == null) {
            return null;
        }

        // Weave invokedynamic patcher into the method visitor chain
        VstmInvokeDynamicPatcher patcher = new VstmInvokeDynamicPatcher(delegate, remapper);
        return new VstmMethodRemapper(patcher, remapper);
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

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        String mappedName = remapper.mapType(name);
        String mappedOuter = (outerName != null) ? remapper.mapType(outerName) : null;
        String mappedInner = (innerName != null) ? remapper.mapInnerClassName(name, outerName, innerName) : null;
        super.visitInnerClass(mappedName, mappedOuter, mappedInner, access);
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
        String mappedOwner = remapper.mapType(owner);
        String mappedName = (name != null) ? remapper.mapMethodName(owner, name, descriptor) : null;
        String mappedDesc = (descriptor != null) ? remapper.mapMethodDesc(descriptor) : null;
        super.visitOuterClass(mappedOwner, mappedName, mappedDesc);
    }

    @Override
    public void visitNestHost(String nestHost) {
        String mappedNestHost = (nestHost != null) ? remapper.mapType(nestHost) : null;
        super.visitNestHost(mappedNestHost);
    }

    @Override
    public void visitNestMember(String nestMember) {
        String mappedNestMember = (nestMember != null) ? remapper.mapType(nestMember) : null;
        super.visitNestMember(mappedNestMember);
    }

    @Override
    public void visitPermittedSubclass(String permittedSubclass) {
        String mappedSubclass = (permittedSubclass != null) ? remapper.mapType(permittedSubclass) : null;
        super.visitPermittedSubclass(mappedSubclass);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        String mappedName = remapper.mapRecordComponentName(this.rawClassName, name, descriptor);
        String mappedDesc = remapper.mapDesc(descriptor);
        String mappedSig = (signature != null) ? remapper.mapSignature(signature, true) : null;
        return super.visitRecordComponent(mappedName != null ? mappedName : name, mappedDesc, mappedSig);
    }

    /**
     * Retrieves the original unmapped class name recorded during visit.
     *
     * @return the raw unmapped class name
     */
    public String getRawClassName() {
        return rawClassName;
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
