package dev.vessel.bootstrap.mojang;

import dev.vessel.kernel.compat.bytecode.AsmClassReaderBytecodeSanitizer;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Bytecode transformer targeting Minecraft {@code Explosion} and {@code ServerExplosion} classes
 * to inject GPU/SIMD accelerated spherical raycast calculations into the explosion pipeline.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselExplosionTransformer {

    public static final String EXPLOSION_INTERNAL_NAME = "net/minecraft/world/level/Explosion";
    public static final String SERVER_EXPLOSION_INTERNAL_NAME = "net/minecraft/world/level/ServerExplosion";

    private final AsmClassReaderBytecodeSanitizer sanitizer = new AsmClassReaderBytecodeSanitizer();

    public VesselExplosionTransformer() {
    }

    /**
     * Determines whether the specified class name represents a Minecraft Explosion class.
     *
     * @param className binary or internal class name
     * @return {@code true} if matching
     */
    public static boolean isExplosionClass(String className) {
        if (className == null) {
            return false;
        }
        return className.equals("net.minecraft.world.level.Explosion")
                || className.equals(EXPLOSION_INTERNAL_NAME)
                || className.equals("net.minecraft.world.level.ServerExplosion")
                || className.equals(SERVER_EXPLOSION_INTERNAL_NAME)
                || className.endsWith("level/Explosion")
                || className.endsWith("level/ServerExplosion");
    }

    /**
     * Transforms Minecraft Explosion bytecode to instrument accelerated raycasts.
     *
     * @param originalBytes raw class bytecode
     * @return transformed bytecode
     */
    public byte[] transformExplosion(byte[] originalBytes) {
        Objects.requireNonNull(originalBytes, "originalBytes must not be null");
        try {
            int originalMajor = sanitizer.extractMajorVersion(originalBytes);
            ClassReader reader = sanitizer.createSafeClassReader(originalBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            VesselExplosionClassVisitor visitor = new VesselExplosionClassVisitor(writer);
            reader.accept(visitor, 0);
            byte[] transformed = writer.toByteArray();
            return sanitizer.restoreOriginalMajorVersion(transformed, originalMajor);
        } catch (Exception e) {
            return originalBytes;
        }
    }
}
