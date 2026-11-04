package dev.vessel.kernel.compat.bytecode;

import java.util.Objects;
import org.objectweb.asm.ClassReader;

/**
 * Sanitizes Java class bytecode headers before passing them to ASM 9.x {@link ClassReader}
 * to prevent {@link IllegalArgumentException} (e.g. {@code Unsupported class file major version 69})
 * when running on modern Java runtimes (Java 22, 23, 24, 25+).
 *
 * <p>Temporarily downgrades the class file major version header (bytes 6–7) to Java 21 (major 65)
 * during ASM processing and restores the original runtime version when bytecode is emitted.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class AsmClassReaderBytecodeSanitizer {

    private static final int JAVA_21_MAJOR = 65;

    /**
     * Extracts the class file major version from the raw bytecode header (bytes 6–7).
     *
     * @param classBytes raw class bytecode
     * @return major version integer, or 0 if bytecode is too short
     */
    public int extractMajorVersion(byte[] classBytes) {
        if (classBytes == null || classBytes.length < 8) {
            return 0;
        }
        return ((classBytes[6] & 0xFF) << 8) | (classBytes[7] & 0xFF);
    }

    /**
     * Sanitizes bytecode for ASM by capping any major version exceeding Java 21 (65) down to 65.
     * Returns the original array unmodified if major version is already supported.
     *
     * @param classBytes raw class bytecode
     * @return sanitized bytecode array safe for ASM {@link ClassReader}
     */
    public byte[] sanitizeForAsm(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes must not be null");
        if (classBytes.length < 8) {
            return classBytes;
        }
        int major = extractMajorVersion(classBytes);
        if (major <= JAVA_21_MAJOR) {
            return classBytes;
        }
        byte[] sanitized = classBytes.clone();
        sanitized[6] = 0;
        sanitized[7] = (byte) JAVA_21_MAJOR;
        return sanitized;
    }

    /**
     * Restores the original major version header into the ASM-produced bytecode array.
     *
     * @param asmProducedBytes bytecode array emitted by ASM {@code ClassWriter}
     * @param originalMajorVersion original major version before sanitization
     * @return bytecode array with the original major version header restored
     */
    public byte[] restoreOriginalMajorVersion(byte[] asmProducedBytes, int originalMajorVersion) {
        Objects.requireNonNull(asmProducedBytes, "asmProducedBytes must not be null");
        if (asmProducedBytes.length < 8 || originalMajorVersion <= JAVA_21_MAJOR) {
            return asmProducedBytes;
        }
        byte[] restored = asmProducedBytes.clone();
        restored[6] = (byte) ((originalMajorVersion >> 8) & 0xFF);
        restored[7] = (byte) (originalMajorVersion & 0xFF);
        return restored;
    }

    /**
     * Creates an ASM {@link ClassReader} safely by preprocessing bytecode with version sanitization.
     *
     * @param classBytes raw class bytecode
     * @return new {@link ClassReader} initialized with safe bytecode
     */
    public ClassReader createSafeClassReader(byte[] classBytes) {
        byte[] safeBytes = sanitizeForAsm(classBytes);
        return new ClassReader(safeBytes);
    }
}
