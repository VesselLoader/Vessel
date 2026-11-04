package dev.vessel.bootstrap.mojang;

import dev.vessel.kernel.compat.bytecode.AsmClassReaderBytecodeSanitizer;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Bytecode transformer instrumenting {@code net.minecraft.resources.ResourceKey} with legacy
 * Forge 1.20.1 SRG method shims using {@link VesselResourceKeyClassVisitor}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselResourceKeyTransformer {

    public static final String TARGET_INTERNAL_NAME = "net/minecraft/resources/ResourceKey";
    public static final String TARGET_CLASS_NAME = "net.minecraft.resources.ResourceKey";

    private final AsmClassReaderBytecodeSanitizer sanitizer = new AsmClassReaderBytecodeSanitizer();

    public VesselResourceKeyTransformer() {
    }

    /**
     * Checks if the given class name matches {@code net.minecraft.resources.ResourceKey}.
     *
     * @param className class name with dots or slashes
     * @return {@code true} if matching
     */
    public static boolean isResourceKeyClass(String className) {
        if (className == null) {
            return false;
        }
        String norm = className.replace('/', '.');
        return "net.minecraft.resources.ResourceKey".equals(norm);
    }

    /**
     * Transforms {@code net.minecraft.resources.ResourceKey} bytecode with SRG shims.
     *
     * @param originalBytes raw class bytecode
     * @return transformed class bytecode
     */
    public byte[] transformResourceKey(byte[] originalBytes) {
        Objects.requireNonNull(originalBytes, "originalBytes must not be null");
        try {
            int originalMajor = sanitizer.extractMajorVersion(originalBytes);
            ClassReader reader = sanitizer.createSafeClassReader(originalBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            VesselResourceKeyClassVisitor visitor = new VesselResourceKeyClassVisitor(writer);
            reader.accept(visitor, 0);
            byte[] transformed = writer.toByteArray();
            return sanitizer.restoreOriginalMajorVersion(transformed, originalMajor);
        } catch (Exception e) {
            return originalBytes;
        }
    }
}
