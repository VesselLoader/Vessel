package dev.vessel.kernel.mixin.remap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Scanner and tokenizer for JVM method and field descriptors in Mixin refmap mappings.
 *
 * <p>Splits signatures of format {@code "Lnet/minecraft/class_1234;method_5678(Lparam;)V"}
 * into owner, name, and descriptor tokens for VSTM remapping.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class RefmapMethodDescriptorScanner {

    /**
     * Constructs the scanner.
     */
    public RefmapMethodDescriptorScanner() {
    }

    /**
     * Splits a target refmap string into owner, name, and descriptor components.
     *
     * @param target signature string (e.g. {@code "Lnet/minecraft/world/level/Level;m_46700_()V"})
     * @return list containing [owner, name, descriptor], or empty if format unrecognized
     */
    public List<String> parseSignatureTokens(String target) {
        Objects.requireNonNull(target, "target cannot be null");

        if (!target.startsWith("L") || !target.contains(";")) {
            return Collections.emptyList();
        }

        int semicolonIdx = target.indexOf(';');
        String owner = target.substring(1, semicolonIdx);
        String remainder = target.substring(semicolonIdx + 1);

        int parenIdx = remainder.indexOf('(');
        if (parenIdx == -1) {
            // Field format: Lowner;fieldName:Ltype;
            int colonIdx = remainder.indexOf(':');
            if (colonIdx != -1) {
                String fieldName = remainder.substring(0, colonIdx);
                String fieldDesc = remainder.substring(colonIdx + 1);
                return List.of(owner, fieldName, fieldDesc);
            }
            return List.of(owner, remainder, "");
        }

        String methodName = remainder.substring(0, parenIdx);
        String methodDesc = remainder.substring(parenIdx);
        return List.of(owner, methodName, methodDesc);
    }
}
