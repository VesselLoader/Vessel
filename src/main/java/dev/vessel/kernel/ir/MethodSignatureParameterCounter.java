package dev.vessel.kernel.ir;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * Counts parameter type elements declared within a method generic signature.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MethodSignatureParameterCounter extends SignatureVisitor {

    private int parameterCount = 0;

    public MethodSignatureParameterCounter() {
        super(Opcodes.ASM9);
    }

    @Override
    public SignatureVisitor visitParameterType() {
        parameterCount++;
        return super.visitParameterType();
    }

    public int getParameterCount() {
        return parameterCount;
    }
}
