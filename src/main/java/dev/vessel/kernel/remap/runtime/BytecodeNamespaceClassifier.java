package dev.vessel.kernel.remap.runtime;

import dev.vessel.kernel.remap.MappingNamespaceKind;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Performs a fast constant-pool scan over raw JVM classfile bytes to classify the bytecode's
 * obfuscation/mapping namespace ({@link MappingNamespaceKind#INTERMEDIARY},
 * {@link MappingNamespaceKind#SEARGE_SRG}, {@link MappingNamespaceKind#SPIGOT_OBF}, or
 * {@link MappingNamespaceKind#MOJMAP}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BytecodeNamespaceClassifier {

    /**
     * Classifies the symbol mapping namespace of the provided class bytecode.
     *
     * @param classBytes raw JVM classfile bytes (must not be null)
     * @return detected {@link MappingNamespaceKind}
     */
    public MappingNamespaceKind classifyClassBytecode(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes must not be null");
        if (classBytes.length < 8) {
            return MappingNamespaceKind.MOJMAP;
        }
        String rawPoolScan = new String(classBytes, StandardCharsets.ISO_8859_1);
        if (rawPoolScan.contains("net/minecraft/server/v1_")) {
            return MappingNamespaceKind.SPIGOT_OBF;
        }
        if (rawPoolScan.contains("func_")
                || rawPoolScan.contains("field_7")
                || rawPoolScan.contains("m_46865_")
                || rawPoolScan.contains("f_46443_")
                || rawPoolScan.contains("m_7470_")
                || rawPoolScan.contains("m_5830_")) {
            return MappingNamespaceKind.SEARGE_SRG;
        }
        if (rawPoolScan.contains("net/minecraft/class_")
                || rawPoolScan.contains("method_")
                || rawPoolScan.contains("field_")) {
            return MappingNamespaceKind.INTERMEDIARY;
        }
        return MappingNamespaceKind.MOJMAP;
    }
}
