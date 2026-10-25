package dev.vessel.kernel.vstm.parser;

import java.util.Objects;

/**
 * Normalizes Java-style type signatures from Mojang ProGuard mappings into canonical JVM bytecode descriptors.
 *
 * <p>Converts human-readable Java types (e.g., {@code int}, {@code void}, {@code net.minecraft.world.level.Level[]})
 * into standard JVM internal descriptors (e.g., {@code I}, {@code V}, {@code [Lnet/minecraft/world/level/Level;}).
 * Operates in $O(N)$ string scan without regex overhead for peak ingestion speed (Catalog ref: TRN-0015).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public final class ProGuardDescriptorNormalizer {

    private ProGuardDescriptorNormalizer() {
        // Utility class
    }

    /**
     * Converts a Java type signature into its JVM bytecode descriptor representation.
     *
     * @param javaType the Java source type name
     * @return canonical JVM descriptor string
     */
    public static String toJvmType(String javaType) {
        Objects.requireNonNull(javaType, "javaType must not be null");
        String trimmed = javaType.trim();

        // Strip generic arguments if present
        int genericStart = trimmed.indexOf('<');
        if (genericStart != -1) {
            int genericEnd = trimmed.lastIndexOf('>');
            if (genericEnd > genericStart) {
                trimmed = trimmed.substring(0, genericStart) + trimmed.substring(genericEnd + 1);
                trimmed = trimmed.trim();
            }
        }

        // Count array dimensions
        int arrayDimensions = 0;
        while (trimmed.endsWith("[]")) {
            arrayDimensions++;
            trimmed = trimmed.substring(0, trimmed.length() - 2).trim();
        }

        String baseDescriptor = switch (trimmed) {
            case "void" -> "V";
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            default -> "L" + trimmed.replace('.', '/') + ";";
        };

        if (arrayDimensions == 0) {
            return baseDescriptor;
        }

        StringBuilder sb = new StringBuilder(arrayDimensions + baseDescriptor.length());
        for (int i = 0; i < arrayDimensions; i++) {
            sb.append('[');
        }
        sb.append(baseDescriptor);
        return sb.toString();
    }

    /**
     * Constructs a standard JVM method descriptor from return type and comma-delimited parameters.
     *
     * @param returnType the method return type in Java syntax
     * @param parameters comma-delimited parameter types (e.g. "int,float,java.lang.String")
     * @return standard JVM method descriptor (e.g. "(IFLjava/lang/String;)V")
     */
    public static String toMethodDescriptor(String returnType, String parameters) {
        Objects.requireNonNull(returnType, "returnType must not be null");

        StringBuilder sb = new StringBuilder();
        sb.append('(');

        if (parameters != null && !parameters.isBlank()) {
            String[] params = parameters.split(",");
            for (String param : params) {
                String trimmedParam = param.trim();
                if (!trimmedParam.isEmpty()) {
                    sb.append(toJvmType(trimmedParam));
                }
            }
        }

        sb.append(')');
        sb.append(toJvmType(returnType));
        return sb.toString();
    }
}
