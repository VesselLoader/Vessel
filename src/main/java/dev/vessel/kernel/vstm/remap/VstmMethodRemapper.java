package dev.vessel.kernel.vstm.remap;

import java.util.Objects;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.Remapper;

/**
 * ASM MethodVisitor responsible for rewriting bytecode instructions within method bodies.
 *
 * <p>Remaps method invocation targets (INVOKEVIRTUAL, INVOKESTATIC, INVOKESPECIAL, INVOKEINTERFACE),
 * field access instructions (GETFIELD, PUTFIELD, GETSTATIC, PUTSTATIC), type references (NEW, CHECKCAST,
 * INSTANCEOF), local variables, catch handler exceptions, and stack frames (Catalog ref: USML-0021).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 3)
 */
public final class VstmMethodRemapper extends MethodVisitor {

    private final Remapper remapper;

    /**
     * Constructs a method remapper with default ASM9 API version.
     *
     * @param methodVisitor downstream method visitor delegate
     * @param remapper      active bytecode remapper
     */
    public VstmMethodRemapper(MethodVisitor methodVisitor, Remapper remapper) {
        this(Opcodes.ASM9, methodVisitor, remapper);
    }

    /**
     * Constructs a method remapper with an explicit ASM API version.
     *
     * @param api           ASM API version
     * @param methodVisitor downstream method visitor delegate
     * @param remapper      active bytecode remapper
     */
    public VstmMethodRemapper(int api, MethodVisitor methodVisitor, Remapper remapper) {
        super(api, methodVisitor);
        this.remapper = Objects.requireNonNull(remapper, "remapper must not be null");
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        String mappedOwner = remapper.mapType(owner);
        String mappedName = remapper.mapFieldName(owner, name, descriptor);
        String mappedDesc = remapper.mapDesc(descriptor);
        super.visitFieldInsn(opcode, mappedOwner, mappedName != null ? mappedName : name, mappedDesc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        String mappedOwner = remapper.mapType(owner);
        String mappedName = remapper.mapMethodName(owner, name, descriptor);
        String mappedDesc = remapper.mapMethodDesc(descriptor);
        super.visitMethodInsn(
                opcode,
                mappedOwner != null ? mappedOwner : owner,
                mappedName != null ? mappedName : name,
                mappedDesc != null ? mappedDesc : descriptor,
                isInterface
        );
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        String mappedType = remapper.mapType(type);
        super.visitTypeInsn(opcode, mappedType != null ? mappedType : type);
    }

    @Override
    public void visitLdcInsn(Object value) {
        Object mappedValue = remapper.mapValue(value);
        super.visitLdcInsn(mappedValue);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        String mappedDesc = remapper.mapDesc(descriptor);
        super.visitMultiANewArrayInsn(mappedDesc != null ? mappedDesc : descriptor, numDimensions);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        String mappedType = (type != null) ? remapper.mapType(type) : null;
        super.visitTryCatchBlock(start, end, handler, mappedType);
    }

    @Override
    public void visitLocalVariable(
            String name,
            String descriptor,
            String signature,
            Label start,
            Label end,
            int index
    ) {
        String mappedDesc = remapper.mapDesc(descriptor);
        String mappedSig = (signature != null) ? remapper.mapSignature(signature, true) : null;
        super.visitLocalVariable(name, mappedDesc, mappedSig, start, end, index);
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        Object[] remappedLocals = remapFrameTypes(numLocal, local);
        Object[] remappedStacks = remapFrameTypes(numStack, stack);
        super.visitFrame(type, numLocal, remappedLocals, numStack, remappedStacks);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        AnnotationVisitor delegate = super.visitAnnotationDefault();
        if (delegate == null) {
            return null;
        }
        return new VstmAnnotationRemapper(delegate, remapper);
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
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        String mappedDesc = remapper.mapDesc(descriptor);
        AnnotationVisitor delegate = super.visitParameterAnnotation(parameter, mappedDesc, visible);
        if (delegate == null) {
            return null;
        }
        return new VstmAnnotationRemapper(descriptor, delegate, remapper);
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        String mappedDesc = remapper.mapDesc(descriptor);
        AnnotationVisitor delegate = super.visitInsnAnnotation(typeRef, typePath, mappedDesc, visible);
        if (delegate == null) {
            return null;
        }
        return new VstmAnnotationRemapper(descriptor, delegate, remapper);
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        String mappedDesc = remapper.mapDesc(descriptor);
        AnnotationVisitor delegate = super.visitTryCatchAnnotation(typeRef, typePath, mappedDesc, visible);
        if (delegate == null) {
            return null;
        }
        return new VstmAnnotationRemapper(descriptor, delegate, remapper);
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(
            int typeRef,
            TypePath typePath,
            Label[] start,
            Label[] end,
            int[] index,
            String descriptor,
            boolean visible
    ) {
        String mappedDesc = remapper.mapDesc(descriptor);
        AnnotationVisitor delegate = super.visitLocalVariableAnnotation(
                typeRef, typePath, start, end, index, mappedDesc, visible
        );
        if (delegate == null) {
            return null;
        }
        return new VstmAnnotationRemapper(descriptor, delegate, remapper);
    }

    private Object[] remapFrameTypes(int count, Object[] frameTypes) {
        if (frameTypes == null || count <= 0) {
            return frameTypes;
        }
        Object[] remapped = new Object[count];
        for (int i = 0; i < count; i++) {
            Object item = frameTypes[i];
            if (item instanceof String internalName) {
                String mapped = remapper.mapType(internalName);
                remapped[i] = (mapped != null) ? mapped : internalName;
            } else {
                remapped[i] = item;
            }
        }
        return remapped;
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
