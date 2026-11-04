package dev.vessel.bootstrap.mojang;

import dev.vessel.kernel.compat.bytecode.AsmClassReaderBytecodeSanitizer;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Bytecode transformer instrumenting {@code net.minecraft.network.chat.Style} with
 * Forge 1.20.1 SRG fields and methods using {@link VesselStyleClassVisitor}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselStyleTransformer {

    public static final String TARGET_INTERNAL_NAME = "net/minecraft/network/chat/Style";
    public static final String TARGET_CLASS_NAME = "net.minecraft.network.chat.Style";

    private final AsmClassReaderBytecodeSanitizer sanitizer = new AsmClassReaderBytecodeSanitizer();

    public VesselStyleTransformer() {
    }

    /**
     * Checks if the given class name matches {@code net.minecraft.network.chat.Style}.
     *
     * @param className class name with dots or slashes
     * @return {@code true} if matching
     */
    public static boolean isStyleClass(String className) {
        if (className == null) {
            return false;
        }
        String norm = className.replace('/', '.');
        return "net.minecraft.network.chat.Style".equals(norm);
    }

    /**
     * Transforms {@code net.minecraft.network.chat.Style} bytecode with SRG fields and methods.
     *
     * @param originalBytes raw class bytecode
     * @return transformed class bytecode
     */
    public byte[] transformStyle(byte[] originalBytes) {
        Objects.requireNonNull(originalBytes, "originalBytes must not be null");
        try {
            int originalMajor = sanitizer.extractMajorVersion(originalBytes);
            ClassReader reader = sanitizer.createSafeClassReader(originalBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            VesselStyleClassVisitor visitor = new VesselStyleClassVisitor(writer);
            reader.accept(visitor, 0);
            byte[] transformed = writer.toByteArray();
            return sanitizer.restoreOriginalMajorVersion(transformed, originalMajor);
        } catch (Exception e) {
            return originalBytes;
        }
    }
}
