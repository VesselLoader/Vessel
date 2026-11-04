package dev.vessel.bootstrap.mojang;

import dev.vessel.kernel.compat.bytecode.AsmClassReaderBytecodeSanitizer;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Bytecode transformer that instruments {@code net.minecraft.commands.CommandSourceStack}
 * using {@link VesselCommandSourceStackClassVisitor} to provide backwards-compatible
 * permission and entity accessor methods for Forge 1.20.1 mods.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselCommandSourceStackTransformer {

    public static final String TARGET_INTERNAL_NAME = "net/minecraft/commands/CommandSourceStack";
    public static final String TARGET_CLASS_NAME = "net.minecraft.commands.CommandSourceStack";

    private final AsmClassReaderBytecodeSanitizer sanitizer = new AsmClassReaderBytecodeSanitizer();

    public VesselCommandSourceStackTransformer() {
    }

    /**
     * Checks if the given class name matches {@code net.minecraft.commands.CommandSourceStack}.
     *
     * @param className class name with dots or slashes
     * @return {@code true} if matching
     */
    public static boolean isCommandSourceStackClass(String className) {
        if (className == null) {
            return false;
        }
        String norm = className.replace('/', '.');
        return "net.minecraft.commands.CommandSourceStack".equals(norm);
    }

    /**
     * Transforms {@code net.minecraft.commands.CommandSourceStack} bytecode with compatibility methods.
     *
     * @param originalBytes raw class bytecode
     * @return transformed class bytecode
     */
    public byte[] transformCommandSourceStack(byte[] originalBytes) {
        Objects.requireNonNull(originalBytes, "originalBytes must not be null");
        try {
            int originalMajor = sanitizer.extractMajorVersion(originalBytes);
            ClassReader reader = sanitizer.createSafeClassReader(originalBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            VesselCommandSourceStackClassVisitor visitor =
                    new VesselCommandSourceStackClassVisitor(writer);
            reader.accept(visitor, 0);
            byte[] transformed = writer.toByteArray();
            return sanitizer.restoreOriginalMajorVersion(transformed, originalMajor);
        } catch (Exception e) {
            return originalBytes;
        }
    }
}
