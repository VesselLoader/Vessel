package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM {@link MethodVisitor} that intercepts string constants loaded via {@code LDC} and substitutes
 * {@code "vanilla"} with {@code "VESSEL"} in any obfuscated or legacy Minecraft method.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class VesselServerBrandLdcMethodVisitor extends MethodVisitor {

    public VesselServerBrandLdcMethodVisitor(MethodVisitor parentVisitor) {
        super(Opcodes.ASM9, parentVisitor);
    }

    @Override
    public void visitLdcInsn(Object value) {
        if ("vanilla".equals(value) || "Vanilla".equals(value)) {
            super.visitLdcInsn("VESSEL");
        } else {
            super.visitLdcInsn(value);
        }
    }
}
