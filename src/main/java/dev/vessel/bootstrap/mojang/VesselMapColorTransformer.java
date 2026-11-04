package dev.vessel.bootstrap.mojang;

import dev.vessel.kernel.compat.bytecode.AsmClassReaderBytecodeSanitizer;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Bytecode transformer instrumenting {@code net.minecraft.world.level.material.MapColor} with
 * Forge 1.20.1 SRG fields using {@link VesselMapColorClassVisitor}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselMapColorTransformer {

    public static final String TARGET_INTERNAL_NAME = "net/minecraft/world/level/material/MapColor";
    public static final String TARGET_CLASS_NAME = "net.minecraft.world.level.material.MapColor";

    private final AsmClassReaderBytecodeSanitizer sanitizer = new AsmClassReaderBytecodeSanitizer();

    public VesselMapColorTransformer() {
    }

    /**
     * Checks if the given class name matches {@code net.minecraft.world.level.material.MapColor}.
     *
     * @param className class name with dots or slashes
     * @return {@code true} if matching
     */
    public static boolean isMapColorClass(String className) {
        if (className == null) {
            return false;
        }
        String norm = className.replace('/', '.');
        return "net.minecraft.world.level.material.MapColor".equals(norm);
    }

    /**
     * Transforms {@code net.minecraft.world.level.material.MapColor} bytecode with SRG fields.
     *
     * @param originalBytes raw class bytecode
     * @return transformed class bytecode
     */
    public byte[] transformMapColor(byte[] originalBytes) {
        Objects.requireNonNull(originalBytes, "originalBytes must not be null");
        try {
            int originalMajor = sanitizer.extractMajorVersion(originalBytes);
            ClassReader reader = sanitizer.createSafeClassReader(originalBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            VesselMapColorClassVisitor visitor = new VesselMapColorClassVisitor(writer);
            reader.accept(visitor, 0);
            byte[] transformed = writer.toByteArray();
            return sanitizer.restoreOriginalMajorVersion(transformed, originalMajor);
        } catch (Exception e) {
            return originalBytes;
        }
    }
}
