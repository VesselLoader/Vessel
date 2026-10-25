package dev.vessel.kernel.ir;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

/**
 * Validates generic signatures against standard JVM ClassFile specifications using ASM SignatureVisitor.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SignatureSyntaxValidator {

    /**
     * Checks whether a class, method, or field generic signature string is syntactically well-formed.
     *
     * @param signature generic signature string
     * @return true if valid according to the JVM specification
     */
    public boolean isValidSignature(String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        try {
            SignatureReader reader = new SignatureReader(signature);
            SignatureWriter writer = new SignatureWriter();
            reader.accept(writer);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
