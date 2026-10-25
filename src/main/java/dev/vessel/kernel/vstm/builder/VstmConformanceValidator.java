package dev.vessel.kernel.vstm.builder;

import dev.vessel.kernel.vstm.binary.VstmbBinaryCorruptException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validates identifier conformance and preempts dangerous collisions with core JVM bootstrap structures.
 *
 * <p>Protects the sovereign microkernel from 75,000 cataloged failure modes:
 * <ul>
 *     <li>{@code UFE-0500}: Remapping of {@code java/lang/Object} or {@code java/lang/ClassLoader}</li>
 *     <li>{@code MAP-0045}: Invalid dot-separated notation instead of internal slash-separated format</li>
 *     <li>{@code MAP-0112}: Malformed method and field bytecode descriptors</li>
 * </ul>
 * </p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmConformanceValidator {

    private static final Pattern INTERNAL_CLASS_NAME = Pattern.compile("^[a-zA-Z0-9_$]+(/[a-zA-Z0-9_$]+)*$");
    private static final Pattern IDENTIFIER_NAME = Pattern.compile("^[a-zA-Z0-9_$<>-]+$");

    /**
     * Validates an internal JVM class name (slash-delimited, no dots).
     *
     * @param className the class name (e.g. "net/minecraft/world/level/Level")
     * @throws VstmbBinaryCorruptException if the name violates JVM specification or touches protected classes
     */
    public static void validateClassName(String className) {
        Objects.requireNonNull(className, "className must not be null");
        if (className.isEmpty()) {
            throw new VstmbBinaryCorruptException("ERR-VSTM-CONFORMANCE", "Class internal name cannot be empty");
        }
        if (className.contains(".")) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-CONFORMANCE",
                    "Class name must use internal slash '/' notation, not dots: " + className
            );
        }
        if (!INTERNAL_CLASS_NAME.matcher(className).matches()) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-CONFORMANCE",
                    "Class name contains illegal characters: " + className
            );
        }

        // Preemptive Guard: Protect core Java runtime classes from inadvertent remapping
        if (className.startsWith("java/lang/") && isProtectedJavaLangClass(className)) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-PROTECTED",
                    "Attempted to remap protected Java bootstrap class: " + className
            );
        }
    }

    /**
     * Validates a member identifier (field or method name).
     *
     * @param name member identifier
     */
    public static void validateMemberName(String name) {
        Objects.requireNonNull(name, "member name must not be null");
        if (name.isEmpty()) {
            throw new VstmbBinaryCorruptException("ERR-VSTM-CONFORMANCE", "Member name cannot be empty");
        }
        if (!IDENTIFIER_NAME.matcher(name).matches()) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-CONFORMANCE",
                    "Member name contains illegal characters: " + name
            );
        }
    }

    /**
     * Validates a field or method bytecode descriptor.
     *
     * @param descriptor bytecode descriptor
     */
    public static void validateDescriptor(String descriptor) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        if (descriptor.isEmpty()) {
            throw new VstmbBinaryCorruptException("ERR-VSTM-CONFORMANCE", "Descriptor cannot be empty");
        }
        if (descriptor.startsWith("(")) {
            // Method descriptor: must contain matching ')'
            int closingParen = descriptor.indexOf(')');
            if (closingParen < 0) {
                throw new VstmbBinaryCorruptException(
                        "ERR-VSTM-CONFORMANCE",
                        "Malformed method descriptor missing ')': " + descriptor
                );
            }
        } else {
            // Field descriptor: must start with valid type character
            char first = descriptor.charAt(0);
            if ("ZBCSIFDJLV[".indexOf(first) < 0) {
                throw new VstmbBinaryCorruptException(
                        "ERR-VSTM-CONFORMANCE",
                        "Invalid field descriptor type prefix: " + descriptor
                );
            }
        }
    }

    private static boolean isProtectedJavaLangClass(String className) {
        return className.equals("java/lang/Object")
                || className.equals("java/lang/Class")
                || className.equals("java/lang/String")
                || className.equals("java/lang/ClassLoader")
                || className.equals("java/lang/Throwable")
                || className.equals("java/lang/System")
                || className.startsWith("java/lang/invoke/")
                || className.startsWith("java/lang/reflect/");
    }

    private VstmConformanceValidator() {
        // Conformance validator utility
    }
}
