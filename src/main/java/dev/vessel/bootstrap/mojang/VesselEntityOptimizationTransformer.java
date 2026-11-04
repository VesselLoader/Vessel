package dev.vessel.bootstrap.mojang;

import dev.vessel.kernel.compat.bytecode.AsmClassReaderBytecodeSanitizer;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Bytecode transformer that instruments {@code net.minecraft.world.entity.Entity} to hook
 * {@code tick()} into the sovereign {@link dev.vessel.kernel.optimization.VesselOptimizationRuntimeHooks}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselEntityOptimizationTransformer {

    public static final String TARGET_INTERNAL_NAME = "net/minecraft/world/entity/Entity";
    public static final String TARGET_CLASS_NAME = "net.minecraft.world.entity.Entity";

    private final AsmClassReaderBytecodeSanitizer sanitizer = new AsmClassReaderBytecodeSanitizer();

    public VesselEntityOptimizationTransformer() {
    }

    /**
     * Checks if the given class name matches the target Entity class.
     *
     * @param className binary or internal class name
     * @return true if matching
     */
    public static boolean isEntityClass(String className) {
        if (className == null) {
            return false;
        }
        return TARGET_CLASS_NAME.equals(className) || TARGET_INTERNAL_NAME.equals(className);
    }

    /**
     * Transforms the given bytecode of {@code net.minecraft.world.entity.Entity}.
     *
     * @param originalBytes raw class bytecode
     * @return transformed class bytecode
     */
    public byte[] transformEntity(byte[] originalBytes) {
        Objects.requireNonNull(originalBytes, "originalBytes must not be null");
        try {
            int originalMajor = sanitizer.extractMajorVersion(originalBytes);
            ClassReader reader = sanitizer.createSafeClassReader(originalBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            VesselEntityOptimizationClassVisitor visitor = new VesselEntityOptimizationClassVisitor(writer);
            reader.accept(visitor, 0);
            byte[] transformed = writer.toByteArray();
            return sanitizer.restoreOriginalMajorVersion(transformed, originalMajor);
        } catch (Exception e) {
            return originalBytes;
        }
    }
}
