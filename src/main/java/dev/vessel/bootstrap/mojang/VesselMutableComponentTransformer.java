package dev.vessel.bootstrap.mojang;

import dev.vessel.kernel.compat.bytecode.AsmClassReaderBytecodeSanitizer;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Bytecode transformer instrumenting {@code net.minecraft.network.chat.MutableComponent} with
 * Forge 1.20.1 SRG methods using {@link VesselMutableComponentClassVisitor}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselMutableComponentTransformer {

    public static final String TARGET_INTERNAL_NAME = "net/minecraft/network/chat/MutableComponent";
    public static final String TARGET_CLASS_NAME = "net.minecraft.network.chat.MutableComponent";

    private final AsmClassReaderBytecodeSanitizer sanitizer = new AsmClassReaderBytecodeSanitizer();

    public VesselMutableComponentTransformer() {
    }

    /**
     * Checks if the given class name matches {@code net.minecraft.network.chat.MutableComponent}.
     *
     * @param className class name with dots or slashes
     * @return {@code true} if matching
     */
    public static boolean isMutableComponentClass(String className) {
        if (className == null) {
            return false;
        }
        String norm = className.replace('/', '.');
        return "net.minecraft.network.chat.MutableComponent".equals(norm);
    }

    /**
     * Transforms {@code net.minecraft.network.chat.MutableComponent} bytecode with SRG methods.
     *
     * @param originalBytes raw class bytecode
     * @return transformed class bytecode
     */
    public byte[] transformMutableComponent(byte[] originalBytes) {
        Objects.requireNonNull(originalBytes, "originalBytes must not be null");
        try {
            int originalMajor = sanitizer.extractMajorVersion(originalBytes);
            ClassReader reader = sanitizer.createSafeClassReader(originalBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            VesselMutableComponentClassVisitor visitor = new VesselMutableComponentClassVisitor(writer);
            reader.accept(visitor, 0);
            byte[] transformed = writer.toByteArray();
            return sanitizer.restoreOriginalMajorVersion(transformed, originalMajor);
        } catch (Exception e) {
            return originalBytes;
        }
    }
}
